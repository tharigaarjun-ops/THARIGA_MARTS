package com.tharigamart.controller;

import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.CartDTO;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import com.tharigamart.service.CartService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Server-side shopping cart (login required). */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(currentUser(authentication)));
    }

    /** Body: { "productId": 1, "quantity": 2 } */
    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItem(Authentication authentication,
                                             @RequestBody Map<String, Object> body) {
        Long productId = toLong(body.get("productId"));
        int quantity = body.get("quantity") == null ? 1
                : ((Number) body.get("quantity")).intValue();

        if (productId == null) {
            throw new com.tharigamart.exception.BadRequestException("productId is required");
        }

        return ResponseEntity.ok(cartService.addItem(currentUser(authentication), productId, quantity));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateItem(Authentication authentication,
                                                @PathVariable Long itemId,
                                                @RequestBody Map<String, Object> body) {
        Integer quantity = body.get("quantity") == null ? null
                : ((Number) body.get("quantity")).intValue();
        if (quantity == null) {
            throw new com.tharigamart.exception.BadRequestException("quantity is required");
        }

        return ResponseEntity.ok(cartService.updateQuantity(currentUser(authentication), itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeItem(Authentication authentication,
                                                @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(currentUser(authentication), itemId));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        cartService.clearCart(currentUser(authentication));
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> count(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", cartService.cartCount(currentUser(authentication))));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("Account not found"));
    }
}
