package com.tharigamart.service;

import com.tharigamart.dto.CheckoutRequest;
import com.tharigamart.dto.OrderDTO;
import com.tharigamart.dto.OrderItemDTO;
import com.tharigamart.dto.PaymentDTO;
import com.tharigamart.entity.Coupon;
import com.tharigamart.entity.Order;
import com.tharigamart.entity.OrderItem;
import com.tharigamart.entity.OrderStatus;
import com.tharigamart.entity.Payment;
import com.tharigamart.entity.PaymentStatus;
import com.tharigamart.entity.Product;
import com.tharigamart.entity.Role;
import com.tharigamart.entity.User;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.OutOfStockException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.CouponRepository;
import com.tharigamart.repository.OrderRepository;
import com.tharigamart.repository.OrderItemRepository;
import com.tharigamart.repository.PaymentRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Checkout, stock reservation, coupons, payments and order history. */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_SEQ =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Flat delivery fee, waived for orders at or above this subtotal. */
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("49.00");
    private static final BigDecimal FREE_DELIVERY_ABOVE = new BigDecimal("499.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        PaymentRepository paymentRepository,
                        CouponRepository couponRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
    }

    /**
     * Places an order atomically: every requested quantity is stock-checked,
     * stock is decremented and order, items and payment are persisted in one
     * transaction. The signed-in buyer becomes the order owner.
     */
    @Transactional
    public OrderDTO placeOrder(String userEmail, CheckoutRequest request) {
        User buyer = requireUser(userEmail);

        // Merge duplicate product lines coming from the client
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CheckoutRequest.CheckoutItem item : request.getItems()) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        Order order = new Order();
        order.setBuyer(buyer);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setOrderNumber(generateOrderNumber());

        CheckoutRequest.Address address = request.getShipping();
        order.setShipName(address.getName().trim());
        order.setShipPhone(address.getPhone());
        order.setShipLine1(address.getLine1().trim());
        order.setShipLine2(blankToNull(address.getLine2()));
        order.setShipCity(address.getCity().trim());
        order.setShipState(address.getState().trim());
        order.setShipPincode(address.getPincode());
        order.setShippingAddressText(formatAddress(address));
        order.setPlacedAt(LocalDateTime.now());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int requested = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

            if (!product.isActive()) {
                throw new BadRequestException("\"" + product.getTitle() + "\" is no longer available");
            }

            if (product.getStockQuantity() < requested) {
                throw new OutOfStockException(product.getTitle(), product.getStockQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - requested);
            productRepository.save(product);

            OrderItem item = new OrderItem(product, requested);
            order.addItem(item);
            subtotal = subtotal.add(item.getSubtotal());
        }

        // Optional coupon discount
        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = blankToNull(request.getCouponCode());
        if (couponCode != null) {
            discount = applyCoupon(couponCode, subtotal);
            order.setCouponCode(couponCode.toUpperCase());
        }

        BigDecimal shipping = deliveryFee(subtotal);
        order.setTotalAmount(subtotal);
        order.setShippingAmount(shipping);
        order.setDiscountAmount(discount);
        order.setFinalAmount(subtotal.add(shipping).subtract(discount));

        // Demo gateway: UPI payments are captured immediately, COD stays pending
        boolean upi = "UPI".equalsIgnoreCase(request.getPaymentMethod())
                || "UPI_QR".equalsIgnoreCase(request.getPaymentMethod());
        order.setPaymentStatus(upi ? PaymentStatus.COMPLETED : PaymentStatus.PENDING);

        Order saved = orderRepository.save(order);

        Payment payment = new Payment(
                saved,
                "TXN-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(10000, 99999),
                saved.getPaymentMethod(),
                saved.getFinalAmount(),
                saved.getPaymentStatus());
        paymentRepository.save(payment);

        return toDto(saved);
    }

    /** Validates a coupon against the subtotal and returns the discount. */
    private BigDecimal applyCoupon(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Coupon code is not valid"));

        if (!coupon.isActive()) {
            throw new BadRequestException("Coupon code is no longer active");
        }
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Coupon code has expired");
        }
        if (coupon.getMinOrderAmount() != null
                && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Coupon requires a minimum order of "
                    + coupon.getMinOrderAmount() + " before delivery charges");
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == com.tharigamart.entity.CouponDiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
            if (discount.compareTo(subtotal) > 0) {
                discount = subtotal;
            }
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Buyer may cancel while the order is still being prepared; stock is restored. */
    @Transactional
    public OrderDTO cancelOrder(String userEmail, Long orderId, String reason) {
        User user = requireUser(userEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));

        boolean owner = order.getBuyer() != null && order.getBuyer().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You can only cancel your own orders");
        }

        if (order.getStatus() != OrderStatus.ORDER_PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("An order in \"" + label(order.getStatus())
                    + "\" status can no longer be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(blankToNull(reason) == null ? "Cancelled by customer" : blankToNull(reason));

        // Restore stock for every line
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        // A captured payment is marked refunded
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.findByOrderId(order.getId()).forEach(payment -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            });
        }

        return toDto(orderRepository.save(order));
    }

    /** ₹49 delivery, free at ₹499+. Mirrored by the front-end summary. */
    private BigDecimal deliveryFee(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_DELIVERY_ABOVE) >= 0 ? BigDecimal.ZERO : DELIVERY_FEE;
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> listOrders(String userEmail) {
        User user = requireUser(userEmail);
        return orderRepository.findByBuyerIdOrderByPlacedAtDesc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrder(String userEmail, Long orderId) {
        User user = requireUser(userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));

        boolean owner = order.getBuyer() != null && order.getBuyer().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You can only view your own orders");
        }
        return toDto(order);
    }

    /** Full unfiltered list for the admin dashboard (newest first). */
    @Transactional(readOnly = true)
    public List<OrderDTO> listAllOrders() {
        return orderRepository.findByOrderByPlacedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    /** Any order, admin only (called from /api/admin/**). */
    @Transactional(readOnly = true)
    public OrderDTO getAnyOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        return toDto(order);
    }

    /** Admin order fulfilment: CONFIRM / SHIP / DELIVER / CANCEL. */
    @Transactional
    public OrderDTO updateStatus(Long orderId, String status, User admin) {
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only admins can update order status");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));

        OrderStatus next;
        try {
            next = OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown status: " + status);
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("A " + label(order.getStatus()).toLowerCase()
                    + " order can no longer be updated");
        }
        if (next == OrderStatus.ORDER_PLACED) {
            throw new BadRequestException("Cannot move an order back to ORDER_PLACED");
        }

        order.setStatus(next);
        return toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public long countOrders(String userEmail) {
        return orderRepository.countByBuyerId(requireUser(userEmail).getId());
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private String generateOrderNumber() {
        String number;
        do {
            number = "TM" + LocalDateTime.now().format(ORDER_SEQ) + "-"
                    + (100000 + ThreadLocalRandom.current().nextInt(900000));
        } while (orderRepository.findByOrderNumber(number).isPresent());
        return number;
    }

    private String formatAddress(CheckoutRequest.Address address) {
        return address.getName().trim() + ", " + address.getPhone() + "\n"
                + address.getLine1().trim()
                + (blankToNull(address.getLine2()) == null ? "" : ", " + address.getLine2().trim()) + "\n"
                + address.getCity().trim() + ", " + address.getState().trim()
                + " - " + address.getPincode();
    }

    private String label(OrderStatus status) {
        return status == null ? "NEW" : status.name().replace('_', ' ');
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public OrderDTO toDto(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setPlacedAt(order.getPlacedAt());
        dto.setStatus(order.getStatus() == null ? null : order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingAmount(order.getShippingAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setCouponCode(order.getCouponCode());
        dto.setShipName(order.getShipName());
        dto.setShipPhone(order.getShipPhone());
        dto.setShipLine1(order.getShipLine1());
        dto.setShipLine2(order.getShipLine2());
        dto.setShipCity(order.getShipCity());
        dto.setShipState(order.getShipState());
        dto.setShipPincode(order.getShipPincode());
        dto.setShippingAddressText(order.getShippingAddressText());
        dto.setCancellationReason(order.getCancellationReason());
        if (order.getBuyer() != null) {
            dto.setCustomerName(order.getBuyer().getDisplayName());
            dto.setCustomerEmail(order.getBuyer().getEmail());
        }

        List<OrderItemDTO> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            OrderItemDTO itemDto = new OrderItemDTO();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
            itemDto.setProductTitle(item.getProductTitle());
            itemDto.setProductImageUrl(item.getProductImageUrl());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setSubtotal(item.getSubtotal());
            if (item.getSeller() != null) {
                itemDto.setSellerId(item.getSeller().getId());
                itemDto.setSellerName(item.getSeller().getDisplayName());
            }
            items.add(itemDto);
        }
        dto.setItems(items);
        dto.setItemCount(items.stream().mapToInt(OrderItemDTO::getQuantity).sum());

        // Payment record for the detail view
        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            PaymentDTO paymentDto = new PaymentDTO();
            paymentDto.setId(payment.getId());
            paymentDto.setTransactionId(payment.getTransactionId());
            paymentDto.setPaymentMethod(payment.getPaymentMethod());
            paymentDto.setAmount(payment.getAmount());
            paymentDto.setStatus(payment.getStatus() == null ? null : payment.getStatus().name());
            paymentDto.setPaymentGatewayRef(payment.getPaymentGatewayRef());
            paymentDto.setPaidAt(payment.getPaidAt());
            paymentDto.setCreatedAt(payment.getCreatedAt());
            dto.setPayment(paymentDto);
        }

        return dto;
    }
}
