package com.example.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** items is optional: when present (Buy now) the coupon is checked against these products instead of the cart. */
public record CouponValidateRequest(
        @NotBlank(message = "Coupon code is required") String code,
        @Valid List<CartItemRequest> items
) {}
