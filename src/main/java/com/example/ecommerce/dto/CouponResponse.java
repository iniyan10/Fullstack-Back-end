package com.example.ecommerce.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long id,
        String code,
        String discountType,
        BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscount,
        Integer maxUses,
        int usedCount,
        Instant expiresAt,
        boolean active
) {}
