package com.example.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Checkout payload.
 *
 * - paymentMethod: CARD | UPI | COD
 * - Card number/expiry/CVV are deliberately NOT part of this model: the browser sends only the last
 *   digits in "card" (and a display-only "cardBrand"); nothing else is read or stored.
 * - items: optional. When present ("Buy now") only these products are ordered and the cart is left
 *   untouched; when absent the whole cart is ordered.
 * - couponCode: optional.
 */
public record CheckoutRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email is invalid") String email,
        @NotBlank(message = "Address is required") String address,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "Pincode is required") String zip,
        @NotBlank(message = "Please choose a payment method") String paymentMethod,
        String card,
        String cardBrand,
        String upiId,
        String couponCode,
        @Valid List<CartItemRequest> items
) {}
