package com.example.ecommerce.entity;

public enum PaymentStatus {
    /** Cash on delivery: money is collected when the order is delivered. */
    PENDING("Pending"),
    PAID("Paid"),
    /** A paid order was cancelled and the money is being returned. */
    REFUNDED("Refunded"),
    /** A cash-on-delivery order was cancelled before any money changed hands. */
    VOIDED("Not charged");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
