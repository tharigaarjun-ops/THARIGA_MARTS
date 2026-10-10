package com.tharigamart.controller;

import com.tharigamart.dto.CouponResultDTO;
import com.tharigamart.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** Coupon validation at checkout (public - no private data is exposed). */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final WishlistService wishlistService;

    public CouponController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /** GET /api/coupons/validate?code=KAWAII10&subtotal=499.00 */
    @GetMapping("/validate")
    public ResponseEntity<CouponResultDTO> validate(
            @RequestParam String code,
            @RequestParam(defaultValue = "0") BigDecimal subtotal) {
        return ResponseEntity.ok(wishlistService.checkCoupon(code, subtotal));
    }
}
