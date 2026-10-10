package com.tharigamart.service;

import com.tharigamart.dto.ProductDTO;
import com.tharigamart.dto.ProductPageResponse;
import com.tharigamart.dto.ProductRequest;
import com.tharigamart.entity.Category;
import com.tharigamart.entity.Product;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.CategoryRepository;
import com.tharigamart.repository.OrderItemRepository;
import com.tharigamart.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Catalogue search, details and admin CRUD for products. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public ProductPageResponse search(String query, String categorySlug, BigDecimal minPrice,
                                      BigDecimal maxPrice, String sort, int page, int size) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price");
        }

        Long categoryId = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            categoryId = categoryRepository.findBySlug(categorySlug.trim())
                    .orElseThrow(() -> ResourceNotFoundException.of("Category", categorySlug))
                    .getId();
        }

        String q = (query == null || query.isBlank()) ? null : query.trim().toLowerCase();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 48);

        Pageable pageable = PageRequest.of(safePage, safeSize, sortSpec(sort));
        Page<Product> result = productRepository.search(q, categoryId, minPrice, maxPrice, pageable);

        return new ProductPageResponse(
                result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductPageResponse featured(int page, int size) {
        Page<Product> result = productRepository
                .findByFeaturedTrue(PageRequest.of(Math.max(0, page), Math.max(1, size),
                        Sort.by("createdAt").descending()));
        return new ProductPageResponse(
                result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> related(Long categoryId, Long excludeId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .filter(p -> !p.getId().equals(excludeId))
                .limit(4)
                .map(this::toDto)
                .toList();
    }

    /* ------------------------- admin CRUD ------------------------- */

    /** Full catalogue for the admin dashboard (newest first). */
    @Transactional(readOnly = true)
    public List<ProductDTO> listAll() {
        return productRepository.findAll(Sort.by("createdAt").descending()).stream()
                .map(this::toDto)
                .toList();
    }

    /** Create a new product (admin only, enforced by URL rules). */
    @Transactional
    public ProductDTO create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDTO update(Long id, ProductRequest request) {
        Product product = requireProduct(id);
        apply(product, request);
        return toDto(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = requireProduct(id);
        if (orderItemRepository.countByProductId(id) > 0) {
            throw new BadRequestException(
                    "\"" + product.getName() + "\" has already been ordered and cannot be deleted. "
                            + "Set its stock to 0 instead.");
        }
        productRepository.delete(product);
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.getCategoryId()));

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription().trim());
        product.setBrand(request.getBrand().trim());
        product.setPrice(request.getPrice());
        product.setMrp(request.getMrp() == null || request.getMrp().compareTo(request.getPrice()) < 0
                ? request.getPrice() : request.getMrp());
        product.setStock(request.getStock());
        product.setImage(request.getImage() == null || request.getImage().isBlank()
                ? "/images/stationery/stationery.svg" : request.getImage().trim());
        product.setFeatured(request.isFeatured());
        product.setCategory(category);
    }

    private Sort sortSpec(String sort) {
        if (sort == null) {
            return Sort.by("createdAt").descending();
        }
        return switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "name_asc" -> Sort.by("name").ascending();
            case "name_desc" -> Sort.by("name").descending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    public ProductDTO toDto(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBrand(product.getBrand());
        dto.setPrice(product.getPrice());
        dto.setMrp(product.getMrp());
        dto.setStock(product.getStock());
        dto.setInStock(product.getStock() > 0);
        dto.setFeatured(product.isFeatured());
        dto.setImage(product.getImage());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
            dto.setCategorySlug(product.getCategory().getSlug());
        }
        dto.setDiscountPercent(discountPercent(product));
        return dto;
    }

    private int discountPercent(Product product) {
        if (product.getMrp() == null || product.getPrice() == null
                || product.getMrp().compareTo(product.getPrice()) <= 0) {
            return 0;
        }
        BigDecimal diff = product.getMrp().subtract(product.getPrice());
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(product.getMrp(), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
