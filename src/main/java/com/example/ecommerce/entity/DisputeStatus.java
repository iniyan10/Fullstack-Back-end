package com.example.ecommerce.entity;

public enum DisputeStatus {
    OPEN, RESOLVED;

    public String label() {
        return name().toLowerCase();
    }
}
