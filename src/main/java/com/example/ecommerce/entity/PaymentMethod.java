package com.example.ecommerce.entity;

public enum PaymentMethod {
    CARD("Credit / Debit Card"),
    UPI("UPI"),
    COD("Cash on Delivery");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
