package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateQuantityRequest(@NotNull(message = "quantity is required") Integer quantity) {}
