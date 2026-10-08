package com.example.ecommerce.controller;

import com.example.ecommerce.dto.CouponValidateRequest;
import com.example.ecommerce.dto.CouponValidationResponse;
import com.example.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /** Checks a coupon against the caller's cart and returns the discount it would give. */
    @PostMapping("/validate")
    public CouponValidationResponse validate(Principal principal, @Valid @RequestBody CouponValidateRequest request) {
        return couponService.preview(principal.getName(), request.code(), request.items());
    }
}
