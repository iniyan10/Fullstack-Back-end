package com.example.ecommerce.dto;

import java.math.BigDecimal;

/** Same shape as the frontend's CartItem type: Product fields plus quantity (id = product id). */
public record CartItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category,
        String type,
        double rating,
        int reviews,
        String image,
        int quantity
) {}
