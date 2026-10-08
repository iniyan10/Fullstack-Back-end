package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category,
        String type,
        double rating,
        int reviews,
        String image,
        int stock,
        boolean active
) {}
