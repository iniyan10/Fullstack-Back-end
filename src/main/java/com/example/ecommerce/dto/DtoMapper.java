package com.example.ecommerce.dto;

import com.example.ecommerce.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class DtoMapper {

    /** Promised delivery window, counted from the order date. */
    public static final long ESTIMATED_DELIVERY_DAYS = 5;

    private DtoMapper() {}

    public static UserDto toUserDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().label(), u.getStatus().label(),
                u.getPhone(), u.getAddress(), u.getCity(), u.getZip());
    }

    public static ProductResponse toProductDto(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory(), p.getType(), p.getRating(), p.getReviews(), p.getImage(), p.getStock(),
                p.isActive());
    }

    public static CartItemResponse toCartItemDto(CartItem ci) {
        Product p = ci.getProduct();
        return new CartItemResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory(), p.getType(), p.getRating(), p.getReviews(), p.getImage(), ci.getQuantity());
    }

    public static CartResponse toCartDto(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted((a, b) -> Long.compare(a.getId() == null ? 0 : a.getId(), b.getId() == null ? 0 : b.getId()))
                .map(DtoMapper::toCartItemDto)
                .toList();
        BigDecimal total = cart.getItems().stream()
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, total);
    }

    public static OrderResponse toOrderDto(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(oi -> new OrderItemResponse(oi.getProduct().getId(), oi.getProductName(),
                        oi.getUnitPrice(), oi.getProduct().getImage(), oi.getQuantity()))
                .toList();
        BillingDetailsResponse billing = new BillingDetailsResponse(o.getBillingName(), o.getBillingEmail(),
                o.getAddress(), o.getCity(), o.getZip(), o.getCardLast4());

        // Orders created before coupons existed have subtotal 0 and no discount: fall back to the total.
        BigDecimal discount = o.getDiscount() == null ? BigDecimal.ZERO : o.getDiscount();
        BigDecimal subtotal = (o.getSubtotal() == null || o.getSubtotal().signum() == 0)
                ? o.getTotal().add(discount) : o.getSubtotal();

        // Orders created before tracking existed have no events: synthesise one from the current state.
        List<OrderEventResponse> timeline = o.getEvents().isEmpty()
                ? List.of(new OrderEventResponse(o.getStatus().label(), "Order placed", o.getCreatedAt()))
                : o.getEvents().stream()
                    .map(e -> new OrderEventResponse(e.getStatus().label(), e.getNote(), e.getCreatedAt()))
                    .toList();

        List<String> nextStatuses = o.getStatus().next().stream().map(OrderStatus::label).toList();

        // Orders placed before payment methods existed only have a card number suffix (or nothing).
        PaymentMethod method = o.getPaymentMethod();
        boolean legacyCard = method == null && o.getCardLast4() != null;
        String methodCode = method != null ? method.name() : (legacyCard ? PaymentMethod.CARD.name() : null);
        String methodLabel = method != null ? method.label() : (legacyCard ? PaymentMethod.CARD.label() : "Not recorded");
        String paymentStatus = o.getPaymentStatus() != null ? o.getPaymentStatus().label() : (legacyCard ? PaymentStatus.PAID.label() : "-");
        String paymentDetail = o.getPaymentDetail() != null ? o.getPaymentDetail()
                : (legacyCard ? "Card \u2022\u2022\u2022\u2022 " + o.getCardLast4() : null);

        boolean open = o.getStatus() == OrderStatus.PROCESSING || o.getStatus() == OrderStatus.SHIPPED;
        Instant estimatedDelivery = open ? o.getCreatedAt().plus(ESTIMATED_DELIVERY_DAYS, ChronoUnit.DAYS) : null;

        return new OrderResponse(o.getId(), o.getCreatedAt(), items, subtotal, discount, o.getCouponCode(),
                o.getTotal(), o.getStatus().label(), billing, o.getUser().getId(), o.getUser().getName(),
                timeline, o.getStatus() == OrderStatus.PROCESSING, nextStatuses,
                methodLabel, methodCode, paymentStatus, paymentDetail, estimatedDelivery);
    }

    public static DisputeResponse toDisputeDto(Dispute d) {
        return new DisputeResponse(d.getId(), d.getOrder().getId(), d.getCustomer().getId(), d.getSellerId(),
                d.getSubject(), d.getDescription(), d.getStatus().label(), d.getResolution(), d.getCreatedAt());
    }

    public static ReviewResponse toReviewDto(Review r, boolean verifiedPurchase) {
        return new ReviewResponse(r.getId(), r.getProduct().getId(), r.getUser().getId(), r.getUser().getName(),
                r.getRating(), r.getComment(), verifiedPurchase, r.getCreatedAt());
    }

    public static CouponResponse toCouponDto(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getDiscountType().name(), c.getValue(),
                c.getMinOrderAmount(), c.getMaxDiscount(), c.getMaxUses(), c.getUsedCount(),
                c.getExpiresAt(), c.isActive());
    }
}
