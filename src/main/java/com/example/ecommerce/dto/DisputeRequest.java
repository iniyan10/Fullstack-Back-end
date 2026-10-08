package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public record DisputeRequest(
        @NotBlank(message = "Subject is required") String subject,
        @NotBlank(message = "Description is required") String description
) {}
