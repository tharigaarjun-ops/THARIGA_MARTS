package com.tharigamart.controller;

import com.tharigamart.dto.AdminStatsDTO;
import com.tharigamart.dto.AdminUserDTO;
import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.OrderDTO;
import com.tharigamart.dto.UpdateOrderStatusRequest;
import com.tharigamart.dto.UpdateUserRoleRequest;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import com.tharigamart.service.AdminService;
import com.tharigamart.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Admin dashboard endpoints (ROLE_ADMIN required). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService,
                             OrderService orderService,
                             UserRepository userRepository) {
        this.adminService = adminService;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> stats(Authentication authentication) {
        adminService.requireAdmin(currentUser(authentication));
        return ResponseEntity.ok(adminService.stats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> listUsers(Authentication authentication) {
        adminService.requireAdmin(currentUser(authentication));
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<AdminUserDTO> updateUserRole(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody UpdateUserRoleRequest request) {
        User admin = currentUser(authentication);
        return ResponseEntity.ok(adminService.updateUserRole(id, request.getRole(), admin));
    }

    @PutMapping("/users/{id}/enabled")
    public ResponseEntity<AdminUserDTO> updateUserEnabled(Authentication authentication,
                                                            @PathVariable Long id,
                                                            @RequestBody Map<String, Boolean> body) {
        User admin = currentUser(authentication);
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new com.tharigamart.exception.BadRequestException("enabled is required");
        }
        return ResponseEntity.ok(adminService.updateUserEnabled(id, enabled, admin));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> listAllOrders(Authentication authentication) {
        adminService.requireAdmin(currentUser(authentication));
        return ResponseEntity.ok(orderService.listAllOrders());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(Authentication authentication,
                                                          @PathVariable Long id) {
        adminService.requireAdmin(currentUser(authentication));
        return ResponseEntity.ok(ApiResponse.ok("Order loaded", orderService.getAnyOrder(id)));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(Authentication authentication,
                                                                   @PathVariable Long id,
                                                                   @Valid @RequestBody UpdateOrderStatusRequest request) {
        User admin = currentUser(authentication);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated",
                orderService.updateStatus(id, request.getStatus(), admin)));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("Account not found"));
    }
}
