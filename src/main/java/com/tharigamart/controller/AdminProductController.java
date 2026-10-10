package com.tharigamart.controller;

import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.ProductDTO;
import com.tharigamart.dto.ProductRequest;
import com.tharigamart.dto.StockUpdateRequest;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import com.tharigamart.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

/**
 * Admin-only product management. ROLE_ADMIN is enforced by
 * SecurityConfig URL rules; ownership rules are enforced in ProductService.
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public AdminProductController(ProductService productService,
                                    UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> list() {
        return ResponseEntity.ok(productService.listAll());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> create(Authentication authentication,
                                                            @Valid @RequestBody ProductRequest request) {
        ProductDTO created = productService.create(request, currentUser(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> update(Authentication authentication,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Product updated",
                productService.update(id, request, currentUser(authentication))));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductDTO>> updateStock(Authentication authentication,
                                                                  @PathVariable Long id,
                                                                  @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Stock updated",
                productService.updateStock(id, request.getStock(), currentUser(authentication))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication,
                                                      @PathVariable Long id) {
        productService.delete(id, currentUser(authentication));
        return ResponseEntity.ok(ApiResponse.ok("Product deleted", null));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("Account not found"));
    }
}
