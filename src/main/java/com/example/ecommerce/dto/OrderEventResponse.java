package com.example.ecommerce.dto;

import java.time.Instant;

public record OrderEventResponse(String status, String note, Instant date) {}
