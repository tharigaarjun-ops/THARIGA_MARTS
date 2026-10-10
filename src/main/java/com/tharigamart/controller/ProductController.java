package com.tharigamart.controller;

import com.tharigamart.dto.ApiResponse;
import com.tharigamart.dto.ProductDTO;
import com.tharigamart.dto.ProductPageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product endpoints:
 *   GET    /api/products                (public, paged search)
 *   GET    /api/products/featured       (public)
 *   GET    /api/products/{id}           (public)
 *   GET    /api/products/{id}/related   (public)
 *   POST   /api/products                (admin / seller)
 *   PUT    /api/products/{id}           (admin / owner)
 *   PUT    /api/products/{id}/stock     (admin / owner)
 *   DELETE /api/products/{id}           (admin / owner)
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService,
                               UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ProductPageResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size) {
        return ResponseEntity.ok(productService.search(search, category, minPrice, maxPrice, sort, page, size));
    }

    @GetMapping("/featured")
    public ResponseEntity<ProductPageResponse> featured(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "8") int size) {
        return ResponseEntity.ok(productService.featured(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Product loaded",
                productService.toDto(productService.requireProduct(id))));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<ProductDTO>> related(@PathVariable Long id) {
        ProductDTO product = productService.toDto(productService.requireProduct(id));
        return ResponseEntity.ok(productService.related(product.getCategoryId(), id));
    }

    /** Admin or seller listing. */
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

    /** Safe stock correction. */
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
