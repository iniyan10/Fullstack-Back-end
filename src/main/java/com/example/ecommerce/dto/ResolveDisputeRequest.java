package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveDisputeRequest(@NotBlank(message = "Resolution is required") String resolution) {}
