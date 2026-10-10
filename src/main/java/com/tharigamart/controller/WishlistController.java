package com.tharigamart.controller;

import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.WishlistItemDTO;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import com.tharigamart.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Per-user wish list (login required). */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    public WishlistController(WishlistService wishlistService, UserRepository userRepository) {
        this.wishlistService = wishlistService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemDTO>> list(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.list(currentUser(authentication)));
    }

    /** Toggles a product in the wish list. Returns { "inWishlist": true|false }. */
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggle(Authentication authentication,
                                                                   @PathVariable Long productId) {
        boolean inWishlist = wishlistService.toggle(currentUser(authentication), productId);
        return ResponseEntity.ok(ApiResponse.ok(
                inWishlist ? "Added to wishlist" : "Removed from wishlist",
                Map.of("inWishlist", inWishlist)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> remove(Authentication authentication,
                                                    @PathVariable Long productId) {
        wishlistService.remove(currentUser(authentication), productId);
        return ResponseEntity.ok(ApiResponse.ok("Removed from wishlist", null));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("Account not found"));
    }
}
