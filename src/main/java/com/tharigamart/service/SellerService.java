package com.tharigamart.service;

import com.tharigamart.dto.OrderDTO;
import com.tharigamart.dto.SellerProfileDTO;
import com.tharigamart.dto.SellerProfileRequest;
import com.tharigamart.dto.SellerStatsDTO;
import com.tharigamart.entity.OrderItem;
import com.tharigamart.entity.Role;
import com.tharigamart.entity.SellerProfile;
import com.tharigamart.entity.SellerProfileStatus;
import com.tharigamart.entity.User;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.OrderItemRepository;
import com.tharigamart.repository.OrderRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.SellerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Seller dashboard: store profile, own products, sales. */
@Service
public class SellerService {

    private final SellerProfileRepository sellerProfileRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public SellerService(SellerProfileRepository sellerProfileRepository,
                           ProductRepository productRepository,
                           OrderItemRepository orderItemRepository,
                           OrderRepository orderRepository) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    /** Requires a ROLE_SELLER account. */
    public User requireSeller(User currentUser) {
        if (currentUser.getRole() != Role.ROLE_SELLER) {
            throw new BadRequestException("This page is for seller accounts");
        }
        return currentUser;
    }

    @Transactional(readOnly = true)
    public SellerProfileDTO getProfile(User currentUser) {
        requireSeller(currentUser);
        return sellerProfileRepository.findByUserId(currentUser.getId())
                .map(this::toDto)
                .orElse(null);
    }

    /** Creates or updates the seller's store profile. */
    @Transactional
    public SellerProfileDTO saveProfile(User currentUser, SellerProfileRequest request) {
        requireSeller(currentUser);

        SellerProfile profile = sellerProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> new SellerProfile(currentUser,
                        request.getStoreName().trim(),
                        blankToNull(request.getStoreDescription()),
                        blankToNull(request.getBusinessAddress()),
                        blankToNull(request.getGstin())));

        profile.setStoreName(request.getStoreName().trim());
        profile.setStoreDescription(blankToNull(request.getStoreDescription()));
        profile.setBusinessAddress(blankToNull(request.getBusinessAddress()));
        profile.setGstin(blankToNull(request.getGstin()));
        if (profile.getStatus() == null) {
            profile.setStatus(SellerProfileStatus.PENDING_APPROVAL);
        }

        return toDto(sellerProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<com.tharigamart.dto.ProductDTO> myProducts(User currentUser) {
        requireSeller(currentUser);
        return productRepository.findBySellerId(currentUser.getId()).stream()
                .map(product -> productServiceToDto(product))
                .toList();
    }

    /** Orders that contain at least one line from this seller. */
    @Transactional(readOnly = true)
    public List<OrderDTO> myOrders(User currentUser) {
        requireSeller(currentUser);
        List<OrderItem> lines = orderItemRepository.findBySellerId(currentUser.getId());

        Map<Long, OrderDTO> orders = new LinkedHashMap<>();
        List<com.tharigamart.entity.Order> all = orderRepository.findAll();
        for (com.tharigamart.entity.Order order : all) {
            List<OrderItem> mine = new ArrayList<>();
            for (OrderItem line : order.getItems()) {
                if (line.getSeller() != null
                        && line.getSeller().getId().equals(currentUser.getId())) {
                    mine.add(line);
                }
            }
            if (mine.isEmpty()) {
                continue;
            }

            com.tharigamart.dto.OrderDTO dto = new com.tharigamart.dto.OrderDTO();
            dto.setId(order.getId());
            dto.setOrderNumber(order.getOrderNumber());
            dto.setPlacedAt(order.getPlacedAt());
            dto.setStatus(order.getStatus() == null ? null : order.getStatus().name());
            dto.setPaymentMethod(order.getPaymentMethod());
            dto.setPaymentStatus(order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
            dto.setCustomerName(order.getBuyer() == null ? null : order.getBuyer().getDisplayName());
            dto.setCustomerEmail(order.getBuyer() == null ? null : order.getBuyer().getEmail());

            List<com.tharigamart.dto.OrderItemDTO> itemDtos = new ArrayList<>();
            BigDecimal sellerSubtotal = BigDecimal.ZERO;
            for (OrderItem line : mine) {
                com.tharigamart.dto.OrderItemDTO itemDto = new com.tharigamart.dto.OrderItemDTO();
                itemDto.setId(line.getId());
                itemDto.setProductId(line.getProduct() == null ? null : line.getProduct().getId());
                itemDto.setProductTitle(line.getProductTitle());
                itemDto.setProductImageUrl(line.getProductImageUrl());
                itemDto.setUnitPrice(line.getUnitPrice());
                itemDto.setQuantity(line.getQuantity());
                itemDto.setSubtotal(line.getSubtotal());
                sellerSubtotal = sellerSubtotal.add(line.getSubtotal());
                itemDtos.add(itemDto);
            }
            dto.setItems(itemDtos);
            dto.setItemCount(itemDtos.stream().mapToInt(com.tharigamart.dto.OrderItemDTO::getQuantity).sum());
            dto.setTotalAmount(sellerSubtotal);
            orders.put(order.getId(), dto);
        }

        return new ArrayList<>(orders.values());
    }

    @Transactional(readOnly = true)
    public SellerStatsDTO stats(User currentUser) {
        requireSeller(currentUser);
        long productCount = productRepository.countBySellerId(currentUser.getId());
        List<OrderItem> lines = orderItemRepository.findBySellerId(currentUser.getId());

        BigDecimal revenue = BigDecimal.ZERO;
        long itemCount = 0;
        for (OrderItem line : lines) {
            revenue = revenue.add(line.getSubtotal());
            itemCount += line.getQuantity();
        }

        return new SellerStatsDTO(productCount, lines.size(), itemCount, revenue);
    }

    public SellerProfileDTO toDto(SellerProfile profile) {
        SellerProfileDTO dto = new SellerProfileDTO();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUser() == null ? null : profile.getUser().getId());
        dto.setStoreName(profile.getStoreName());
        dto.setStoreDescription(profile.getStoreDescription());
        dto.setBusinessAddress(profile.getBusinessAddress());
        dto.setGstin(profile.getGstin());
        dto.setStatus(profile.getStatus() == null ? null : profile.getStatus().name());
        dto.setTotalSales(profile.getTotalSales());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        return dto;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Delegates to ProductService.toDto without a circular dependency. */
    private com.tharigamart.dto.ProductDTO productServiceToDto(com.tharigamart.entity.Product product) {
        com.tharigamart.dto.ProductDTO dto = new com.tharigamart.dto.ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getTitle());
        dto.setSku(product.getSku());
        dto.setDescription(product.getDescription());
        dto.setBrand(product.getBrand());
        dto.setPrice(product.getPrice());
        dto.setMrp(product.getMrp());
        dto.setDiscountPercent(product.getDiscountPercent());
        dto.setStock(product.getStockQuantity());
        dto.setInStock(product.getStockQuantity() > 0 && product.isActive());
        dto.setFeatured(product.isFeatured());
        dto.setBestseller(product.isBestseller());
        dto.setNewArrival(product.isNewArrival());
        dto.setActive(product.isActive());
        dto.setRating(product.getRating());
        dto.setReviewCount(product.getReviewCount() == null ? 0 : product.getReviewCount());
        dto.setImage(product.getImageUrl());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
            dto.setCategorySlug(product.getCategory().getSlug());
        }
        if (product.getSeller() != null) {
            dto.setSellerId(product.getSeller().getId());
            dto.setSellerName(product.getSeller().getDisplayName());
        }
        return dto;
    }
}
