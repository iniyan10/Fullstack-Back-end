package com.example.ecommerce.entity;

public enum Role {
    ADMIN, USER;

    /** Frontend expects "admin" | "user". */
    public String label() {
        return name().toLowerCase();
    }
}
