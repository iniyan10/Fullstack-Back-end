package com.example.ecommerce.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String userName,
        int rating,
        String comment,
        boolean verifiedPurchase,
        Instant createdAt
) {}
