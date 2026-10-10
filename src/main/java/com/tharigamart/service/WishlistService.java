package com.tharigamart.service;

import com.tharigamart.dto.CouponResultDTO;
import com.tharigamart.dto.WishlistItemDTO;
import com.tharigamart.entity.Coupon;
import com.tharigamart.entity.CouponDiscountType;
import com.tharigamart.entity.Product;
import com.tharigamart.entity.User;
import com.tharigamart.entity.Wishlist;
import com.tharigamart.entity.WishlistItem;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.exception.ResourceNotFoundException;
import com.tharigamart.repository.CouponRepository;
import com.tharigamart.repository.ProductRepository;
import com.tharigamart.repository.WishlistItemRepository;
import com.tharigamart.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Coupon validation and the per-user wish list. */
@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                             WishlistItemRepository wishlistItemRepository,
                             ProductRepository productRepository,
                             CouponRepository couponRepository) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
    }

    /* ------------------------- coupon validation ------------------------- */

    /** Checks a coupon code against the subtotal (shown at checkout). */
    @Transactional(readOnly = true)
    public CouponResultDTO checkCoupon(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return CouponResultDTO.invalid("Enter a coupon code");
        }

        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase())
                .orElse(null);

        if (coupon == null) {
            return CouponResultDTO.invalid("Coupon code is not valid");
        }
        if (!coupon.isActive()) {
            return CouponResultDTO.invalid("Coupon code is no longer active");
        }
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            return CouponResultDTO.invalid("Coupon code has expired");
        }
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            return CouponResultDTO.invalid("Coupon requires a minimum order of "
                    + coupon.getMinOrderAmount() + " before delivery charges");
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue().min(subtotal);
        }

        CouponResultDTO result = new CouponResultDTO();
        result.setCode(coupon.getCode());
        result.setValid(true);
        result.setDiscountType(coupon.getDiscountType().name());
        result.setDiscountValue(coupon.getDiscountValue());
        result.setMaxDiscount(coupon.getMaxDiscount());
        result.setMinOrderAmount(coupon.getMinOrderAmount());
        result.setDiscountAmount(discount);
        result.setMessage("Coupon applied - you save " + discount + " on this order");
        return result;
    }

    /* ------------------------- wish list ------------------------- */

    @Transactional
    public Wishlist getOrCreateWishlist(User user) {
        return wishlistRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wishlist wishlist = new Wishlist();
                    wishlist.setUser(user);
                    return wishlistRepository.save(wishlist);
                });
    }

    @Transactional(readOnly = true)
    public List<WishlistItemDTO> list(User user) {
        return wishlistRepository.findByUserId(user.getId())
                .map(wishlist -> wishlist.getItems().stream()
                        .map(this::toDto)
                        .toList())
                .orElseGet(ArrayList::new);
    }

    @Transactional
    public boolean toggle(User user, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        return wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .map(item -> {
                    wishlist.getItems().remove(item);
                    wishlistItemRepository.delete(item);
                    wishlistRepository.save(wishlist);
                    return false;
                })
                .orElseGet(() -> {
                    wishlist.getItems().add(new WishlistItem(wishlist, product));
                    wishlistRepository.save(wishlist);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean contains(User user, Long productId) {
        return wishlistRepository.findByUserId(user.getId())
                .map(wishlist -> wishlistItemRepository
                        .existsByWishlistIdAndProductId(wishlist.getId(), productId))
                .orElse(false);
    }

    @Transactional
    public void remove(User user, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(user);
        wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .ifPresent(item -> {
                    wishlist.getItems().remove(item);
                    wishlistItemRepository.delete(item);
                    wishlistRepository.save(wishlist);
                });
    }

    public WishlistItemDTO toDto(WishlistItem item) {
        Product product = item.getProduct();
        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setId(item.getId());
        dto.setProductId(product == null ? null : product.getId());
        dto.setName(product == null ? null : product.getTitle());
        dto.setImage(product == null ? null : product.getImageUrl());
        dto.setPrice(product == null ? null : product.getPrice());
        dto.setMrp(product == null ? null : product.getMrp());
        dto.setStock(product == null ? 0 : product.getStockQuantity());
        dto.setInStock(product != null && product.getStockQuantity() > 0 && product.isActive());
        dto.setAddedAt(item.getAddedAt());
        return dto;
    }
}
