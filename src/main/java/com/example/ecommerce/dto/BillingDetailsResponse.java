package com.example.ecommerce.dto;

public record BillingDetailsResponse(
        String name,
        String email,
        String address,
        String city,
        String zip,
        String cardLast4
) {}
