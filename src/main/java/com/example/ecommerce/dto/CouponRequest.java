package com.example.ecommerce.dto;

import com.example.ecommerce.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotBlank(message = "Code is required") @Size(max = 50, message = "Code is too long") String code,
        @NotNull(message = "Discount type is required") DiscountType discountType,
        @NotNull(message = "Value is required")
        @DecimalMin(value = "0.01", message = "Value must be greater than 0") BigDecimal value,
        @DecimalMin(value = "0.0", message = "Minimum order cannot be negative") BigDecimal minOrderAmount,
        @DecimalMin(value = "0.0", message = "Max discount cannot be negative") BigDecimal maxDiscount,
        @Min(value = 1, message = "Max uses must be at least 1") Integer maxUses,
        Instant expiresAt,
        Boolean active
) {}
