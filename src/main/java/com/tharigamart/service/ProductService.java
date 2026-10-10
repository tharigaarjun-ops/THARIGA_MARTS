package com.tharigamart.service;

import com.tharigamart.dto.ProductDTO;
import com.tharigamart.dto.ProductPageResponse;
import com.tharigamart.dto.ProductRequest;
import com.tharigamart.entity.Category;
import com.tharigamart.entity.Product;
import com.tharigamart.entity.Role;
import com.tharigamart.entity.User;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.CategoryRepository;
import com.tharigamart.repository.OrderItemRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Catalogue search, details and admin / seller product CRUD. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          OrderItemRepository orderItemRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
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
                .findByActiveTrueAndIsFeaturedTrue(PageRequest.of(Math.max(0, page), Math.max(1, size),
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
                .filter(p -> p.isActive() && !p.getId().equals(excludeId))
                .limit(4)
                .map(this::toDto)
                .toList();
    }

    /* ------------------------- admin CRUD ------------------------- */

    /** Full catalogue for the admin dashboard (newest first). */
    @Transactional(readOnly = true)
    public List<ProductDTO> listAll() {
        return productRepository.findAllForAdmin().stream()
                .map(this::toDto)
                .toList();
    }

    /** Every active product (used by the AI shopping assistant). */
    @Transactional(readOnly = true)
    public List<ProductDTO> listActive() {
        return productRepository.findByActiveTrue().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Create a product. Sellers always list under their own account;
     * admins must name the seller who owns the listing.
     */
    @Transactional
    public ProductDTO create(ProductRequest request, User currentUser) {
        Product product = new Product();
        apply(product, request, currentUser);
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDTO update(Long id, ProductRequest request, User currentUser) {
        Product product = requireProduct(id);
        ensureCanManage(product, currentUser);
        apply(product, request, currentUser);
        return toDto(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Product product = requireProduct(id);
        ensureCanManage(product, currentUser);
        if (orderItemRepository.countByProductId(id) > 0) {
            throw new BadRequestException(
                    "\"" + product.getTitle() + "\" has already been ordered and cannot be deleted. "
                            + "Set its stock to 0 instead.");
        }
        productRepository.delete(product);
    }

    /** Safe stock correction used by the seller / admin dashboards. */
    @Transactional
    public ProductDTO updateStock(Long id, int stock, User currentUser) {
        Product product = requireProduct(id);
        ensureCanManage(product, currentUser);
        product.setStockQuantity(stock);
        return toDto(productRepository.save(product));
    }

    /** Products listed by one seller. */
    @Transactional(readOnly = true)
    public List<ProductDTO> listBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream()
                .map(this::toDto)
                .toList();
    }

    /** A seller can only manage their own listings; an admin can manage all. */
    private void ensureCanManage(Product product, User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = product.getSeller() != null
                && product.getSeller().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new BadRequestException("You can only manage your own products");
        }
    }

    private void apply(Product product, ProductRequest request, User currentUser) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.getCategoryId()));

        User seller = resolveSeller(request, currentUser);

        product.setTitle(request.getName().trim());
        product.setSku(blankToNull(request.getSku()));
        product.setDescription(request.getDescription().trim());
        product.setBrand(request.getBrand() == null || request.getBrand().isBlank()
                ? "" : request.getBrand().trim());
        product.setPrice(request.getPrice());
        product.setMrp(request.getMrp() == null || request.getMrp().compareTo(request.getPrice()) < 0
                ? request.getPrice() : request.getMrp());
        product.setDiscountPercent(request.getMrp() == null ? 0
                : discountPercent(request.getPrice(), request.getMrp()));
        product.setStockQuantity(request.getStock());
        product.setImageUrl(blankToNull(request.getImage()));
        product.setFeatured(request.isFeatured());
        product.setBestseller(request.isBestseller());
        product.setNewArrival(request.isNewArrival());
        product.setActive(request.isActive());
        product.setCategory(category);
        product.setSeller(seller);
    }

    /**
     * Sellers always list under their own account. Admins choose the
     * seller who owns the product.
     */
    private User resolveSeller(ProductRequest request, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_SELLER) {
            return currentUser;
        }

        Long sellerId = request.getSellerId();
        if (sellerId == null) {
            throw new BadRequestException("Select the seller who owns this product");
        }

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Seller", sellerId));
        if (seller.getRole() != Role.ROLE_SELLER) {
            throw new BadRequestException("The selected account is not a seller");
        }
        return seller;
    }

    private Sort sortSpec(String sort) {
        if (sort == null) {
            return Sort.by("createdAt").descending();
        }
        return switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "name_asc" -> Sort.by("title").ascending();
            case "name_desc" -> Sort.by("title").descending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    public ProductDTO toDto(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getTitle());
        dto.setSku(product.getSku());
        dto.setDescription(product.getDescription());
        dto.setBrand(product.getBrand());
        dto.setPrice(product.getPrice());
        dto.setMrp(product.getMrp());
        dto.setDiscountPercent(discountPercent(product.getPrice(), product.getMrp()));
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

    private int discountPercent(BigDecimal price, BigDecimal mrp) {
        if (mrp == null || price == null || mrp.compareTo(price) <= 0) {
            return 0;
        }
        BigDecimal diff = mrp.subtract(price);
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(mrp, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
