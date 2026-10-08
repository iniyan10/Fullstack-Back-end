package com.example.ecommerce.dto;

import java.time.Instant;

public record DisputeResponse(
        Long id,
        Long orderId,
        Long customerId,
        String sellerId,
        String subject,
        String description,
        String status,
        String resolution,
        Instant createdAt
) {}
