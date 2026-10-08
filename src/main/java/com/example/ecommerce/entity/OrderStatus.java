package com.example.ecommerce.entity;

import java.util.List;

public enum OrderStatus {
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** The statuses an order in this state is allowed to move to. */
    public List<OrderStatus> next() {
        switch (this) {
            case PROCESSING:
                return List.of(SHIPPED, CANCELLED);
            case SHIPPED:
                return List.of(DELIVERED);
            default:
                return List.of();
        }
    }
}
