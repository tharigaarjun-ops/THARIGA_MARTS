package com.tharigamart.service;

import com.tharigamart.dto.AdminStatsDTO;
import com.tharigamart.dto.AdminUserDTO;
import com.tharigamart.dto.OrderDTO;
import com.tharigamart.entity.Order;
import com.tharigamart.entity.OrderStatus;
import com.tharigamart.entity.PaymentStatus;
import com.tharigamart.entity.Role;
import com.tharigamart.entity.User;
import com.tharigamart.repository.OrderRepository;
import com.tharigamart.repository.OrderItemRepository;
import com.tharigamart.repository.PaymentRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Admin dashboard: stats, users, all orders. */
@Service
public class AdminService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public AdminService(UserRepository userRepository,
                          ProductRepository productRepository,
                          OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
    }

    public void requireAdmin(User currentUser) {
        if (currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new com.tharigamart.exception.BadRequestException("This page is for admins");
        }
    }

    @Transactional(readOnly = true)
    public AdminStatsDTO stats() {
        long totalUsers = userRepository.count();
        long totalSellers = userRepository.findByRole(Role.ROLE_SELLER).size();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.ORDER_PLACED
                        || o.getStatus() == OrderStatus.CONFIRMED)
                .count();
        long completedPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .count();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() != OrderStatus.CANCELLED
                    && order.getFinalAmount() != null) {
                totalRevenue = totalRevenue.add(order.getFinalAmount());
            }
        }

        return new AdminStatsDTO(totalUsers, totalProducts, totalOrders,
                totalSellers, totalRevenue, pendingOrders, completedPayments);
    }

    @Transactional(readOnly = true)
    public List<AdminUserDTO> listUsers() {
        List<AdminUserDTO> users = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            AdminUserDTO dto = new AdminUserDTO();
            dto.setId(user.getId());
            dto.setName(user.getDisplayName());
            dto.setEmail(user.getEmail());
            dto.setPhone(user.getPhone());
            dto.setRole(user.getRole() == null ? null : user.getRole().name());
            dto.setEnabled(user.isEnabled());
            dto.setCreatedAt(user.getCreatedAt() == null ? null
                    : user.getCreatedAt().format(ISO));
            dto.setOrderCount(orderRepository.countByBuyerId(user.getId()));
            users.add(dto);
        }
        return users;
    }

    /** Admin changes a user's role (never touches the admin's own role). */
    @Transactional
    public AdminUserDTO updateUserRole(Long userId, String role, User admin) {
        requireAdmin(admin);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("User not found: " + userId));

        Role next;
        try {
            String normalized = role.trim().toUpperCase();
            next = Role.valueOf(normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized);
        } catch (IllegalArgumentException ex) {
            throw new com.tharigamart.exception.BadRequestException("Unknown role: " + role);
        }

        if (user.getId().equals(admin.getId()) && next != Role.ROLE_ADMIN) {
            throw new com.tharigamart.exception.BadRequestException("You cannot change your own admin role");
        }

        user.setRole(next);
        userRepository.save(user);

        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setName(user.getDisplayName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt() == null ? null
                : user.getCreatedAt().format(ISO));
        dto.setOrderCount(orderRepository.countByBuyerId(user.getId()));
        return dto;
    }

    /** Admin enables / disables an account. */
    @Transactional
    public AdminUserDTO updateUserEnabled(Long userId, boolean enabled, User admin) {
        requireAdmin(admin);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("User not found: " + userId));

        if (user.getId().equals(admin.getId()) && !enabled) {
            throw new com.tharigamart.exception.BadRequestException("You cannot disable your own admin account");
        }

        user.setEnabled(enabled);
        userRepository.save(user);

        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setName(user.getDisplayName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt() == null ? null
                : user.getCreatedAt().format(ISO));
        dto.setOrderCount(orderRepository.countByBuyerId(user.getId()));
        return dto;
    }
}
