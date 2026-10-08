package com.example.ecommerce.dto;

public record UserDto(
        Long id,
        String name,
        String email,
        String role,
        String status,
        String phone,
        String address,
        String city,
        String zip
) {}
