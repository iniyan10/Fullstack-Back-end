package com.example.ecommerce.entity;

public enum UserStatus {
    PENDING, VERIFIED;

    public String label() {
        return name().toLowerCase();
    }
}
