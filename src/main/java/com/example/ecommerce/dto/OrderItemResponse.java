package com.example.ecommerce.dto;

import java.math.BigDecimal;

/** Shape matches the frontend's CartItem so the Orders page renders unchanged (id = product id). */
public record OrderItemResponse(
        Long id,
        String name,
        BigDecimal price,
        String image,
        int quantity
) {}
