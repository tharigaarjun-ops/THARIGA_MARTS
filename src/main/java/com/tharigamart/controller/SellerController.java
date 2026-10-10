package com.tharigamart.controller;

import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.OrderDTO;
import com.tharigamart.dto.ProductDTO;
import com.tharigamart.dto.ProductRequest;
import com.tharigamart.dto.SellerProfileDTO;
import com.tharigamart.dto.SellerProfileRequest;
import com.tharigamart.dto.SellerStatsDTO;
import com.tharigamart.dto.StockUpdateRequest;
import com.tharigamart.entity.User;
import com.tharigamart.repository.UserRepository;
import com.tharigamart.service.ProductService;
import com.tharigamart.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

/** Seller dashboard endpoints (ROLE_SELLER required). */
@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerService sellerService;
    private final ProductService productService;
    private final UserRepository userRepository;

    public SellerController(SellerService sellerService,
                              ProductService productService,
                              UserRepository userRepository) {
        this.sellerService = sellerService;
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<SellerProfileDTO> profile(Authentication authentication) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        return ResponseEntity.ok(sellerService.getProfile(seller));
    }

    @PostMapping("/profile")
    public ResponseEntity<SellerProfileDTO> saveProfile(Authentication authentication,
                                                          @Valid @RequestBody SellerProfileRequest request) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        return ResponseEntity.ok(sellerService.saveProfile(seller, request));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> myProducts(Authentication authentication) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        return ResponseEntity.ok(sellerService.myProducts(seller));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(Authentication authentication,
                                                                   @Valid @RequestBody ProductRequest request) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        ProductDTO created = productService.create(request, seller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product listed for sale", created));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(Authentication authentication,
                                                                   @PathVariable Long id,
                                                                   @Valid @RequestBody ProductRequest request) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        return ResponseEntity.ok(ApiResponse.ok("Product updated",
                productService.update(id, request, seller)));
    }

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<ApiResponse<ProductDTO>> updateStock(Authentication authentication,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody StockUpdateRequest request) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        return ResponseEntity.ok(ApiResponse.ok("Stock updated",
                productService.updateStock(id, request.getStock(), seller)));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> myOrders(Authentication authentication) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        return ResponseEntity.ok(sellerService.myOrders(seller));
    }

    @GetMapping("/stats")
    public ResponseEntity<SellerStatsDTO> stats(Authentication authentication) {
        User seller = sellerService.requireSeller(currentUser(authentication));
        return ResponseEntity.ok(sellerService.stats(seller));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.tharigamart.exception.ResourceNotFoundException("Account not found"));
    }
}
