package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record CouponValidationResponse(
        String code,
        String description,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
) {}
