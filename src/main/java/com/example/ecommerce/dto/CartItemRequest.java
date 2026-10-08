package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "quantity is required") Integer quantity
) {}
