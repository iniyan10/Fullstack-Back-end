package com.example.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /** Sum of the lines before any coupon. Old orders (before coupons existed) have 0 here. */
    @Column(columnDefinition = "DECIMAL(12,2) NOT NULL DEFAULT 0")
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(columnDefinition = "DECIMAL(12,2) NOT NULL DEFAULT 0")
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(length = 50)
    private String couponCode;

    /** Amount actually charged: subtotal - discount. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<OrderEvent> events = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PROCESSING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Billing / shipping snapshot
    @Column(nullable = false, length = 100)
    private String billingName;
    @Column(nullable = false, length = 150)
    private String billingEmail;
    @Column(nullable = false, length = 255)
    private String address;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, length = 20)
    private String zip;

    /** Only the last 4 digits are ever stored. Full card data is never persisted. */
    @Column(length = 4)
    private String cardLast4;

    /** null on orders placed before payment methods existed. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus paymentStatus;

    /** Human readable, already masked: "Visa •••• 4242" or "ab***@okaxis". */
    @Column(length = 120)
    private String paymentDetail;

    public Order() {}

    public void addEvent(OrderStatus status, String note) {
        OrderEvent event = new OrderEvent();
        event.setOrder(this);
        event.setStatus(status);
        event.setNote(note);
        events.add(event);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public List<OrderEvent> getEvents() { return events; }
    public void setEvents(List<OrderEvent> events) { this.events = events; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }
    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String billingEmail) { this.billingEmail = billingEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentDetail() { return paymentDetail; }
    public void setPaymentDetail(String paymentDetail) { this.paymentDetail = paymentDetail; }
}
