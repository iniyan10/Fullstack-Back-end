package com.example.ecommerce.dto;

/** archived = true when the product was part of past orders and was hidden instead of deleted. */
public record DeleteProductResponse(boolean archived, String message) {}
