package com.example.ecommerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Instant date,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        String couponCode,
        BigDecimal total,
        String status,
        BillingDetailsResponse billingDetails,
        Long customerId,
        String customerName,
        List<OrderEventResponse> timeline,
        boolean canCancel,
        List<String> nextStatuses,
        /** Display label, e.g. "UPI" */
        String paymentMethod,
        /** CARD | UPI | COD, or null for very old orders */
        String paymentMethodCode,
        /** Pending | Paid | Refunded | Not charged */
        String paymentStatus,
        /** Masked detail, e.g. "Visa •••• 4242" */
        String paymentDetail,
        /** null once the order is delivered or cancelled */
        Instant estimatedDelivery
) {}
