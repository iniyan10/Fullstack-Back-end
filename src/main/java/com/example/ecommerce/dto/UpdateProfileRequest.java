package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name is too long") String name,
        @Size(max = 30, message = "Phone number is too long") String phone,
        @Size(max = 255, message = "Address is too long") String address,
        @Size(max = 100, message = "City is too long") String city,
        @Size(max = 20, message = "Pincode is too long") String zip
) {}
