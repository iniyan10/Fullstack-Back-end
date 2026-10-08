package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.CheckoutRequest;
import com.example.ecommerce.dto.DtoMapper;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

@Service
public class OrderService {

    /** name@bank, e.g. rahul.k@okaxis */
    private static final Pattern UPI_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{2,64}@[a-zA-Z][a-zA-Z0-9]{1,63}$");
    private static final Set<String> CARD_BRANDS = Set.of("Visa", "Mastercard", "RuPay", "Amex", "Discover", "Diners");
    private static final String DOTS = "\u2022\u2022\u2022\u2022";
    private static final int MAX_QUANTITY_PER_LINE = 20;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final CouponService couponService;
    private final BigDecimal codLimit;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        ProductRepository productRepository, UserService userService,
                        CouponService couponService,
                        @Value("${app.payment.cod-limit}") BigDecimal codLimit) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userService = userService;
        this.couponService = couponService;
        this.codLimit = codLimit;
    }

    public BigDecimal getCodLimit() {
        return codLimit;
    }

    /**
     * Places an order.
     *
     * Normal checkout orders the whole server-side cart (and empties it). "Buy now" sends explicit
     * items, which are ordered on their own while the cart stays untouched.
     */
    @Transactional
    public OrderResponse placeOrder(String email, CheckoutRequest request) {
        User user = userService.getByEmail(email);
        PaymentMethod method = parseMethod(request.paymentMethod());

        boolean buyNow = request.items() != null && !request.items().isEmpty();
        Cart cart = null;
        // productId -> quantity. A TreeMap keeps a stable lock order (avoids deadlocks between orders).
        Map<Long, Integer> wanted = new TreeMap<>();
        if (buyNow) {
            for (CartItemRequest item : request.items()) {
                if (item.quantity() <= 0 || item.quantity() > MAX_QUANTITY_PER_LINE) {
                    throw ApiException.badRequest("Quantity must be between 1 and " + MAX_QUANTITY_PER_LINE);
                }
                wanted.merge(item.productId(), item.quantity(), Integer::sum);
            }
        } else {
            cart = cartRepository.findByUserId(user.getId())
                    .orElseThrow(() -> ApiException.badRequest("Your cart is empty"));
            if (cart.getItems().isEmpty()) {
                throw ApiException.badRequest("Your cart is empty");
            }
            for (CartItem line : cart.getItems()) {
                wanted.merge(line.getProduct().getId(), line.getQuantity(), Integer::sum);
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setBillingName(request.name().trim());
        order.setBillingEmail(request.email().trim());
        order.setAddress(request.address().trim());
        order.setCity(request.city().trim());
        order.setZip(request.zip().trim());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            // Lock the product row so two concurrent orders cannot oversell stock.
            Product product = productRepository.findByIdForUpdate(entry.getKey())
                    .filter(Product::isActive)
                    .orElseThrow(() -> ApiException.notFound("A product in your order is no longer available"));
            int quantity = entry.getValue();
            if (quantity > product.getStock()) {
                throw ApiException.badRequest("Only " + product.getStock() + " unit(s) of \""
                        + product.getName() + "\" left in stock");
            }
            product.setStock(product.getStock() - quantity);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(quantity);
            order.getItems().add(item);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = request.couponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponService.requireUsable(couponCode, subtotal, user);
            discount = coupon.computeDiscount(subtotal);
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            order.setCouponCode(coupon.getCode());
        }
        BigDecimal total = subtotal.subtract(discount);
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTotal(total);

        applyPayment(order, method, request, total);

        boolean paid = order.getPaymentStatus() == PaymentStatus.PAID;
        order.addEvent(OrderStatus.PROCESSING, paid
                ? "Order placed. Payment received via " + method.label() + "."
                : "Order placed. Pay \u20b9" + money(total) + " in cash on delivery.");

        Order saved = orderRepository.save(order);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
        return DtoMapper.toOrderDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(String email) {
        User user = userService.getByEmail(email);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(DtoMapper::toOrderDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(String email, Long orderId) {
        User user = userService.getByEmail(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        // Someone else's order is reported as 404 so order ids cannot be probed.
        if (!order.getUser().getId().equals(user.getId())) {
            throw ApiException.notFound("Order not found");
        }
        return DtoMapper.toOrderDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> allOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(DtoMapper::toOrderDto).toList();
    }

    /** Customer cancels their own order. Only allowed while it is still being processed. */
    @Transactional
    public OrderResponse cancelMine(String email, Long orderId, String reason) {
        User user = userService.getByEmail(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw ApiException.notFound("Order not found");
        }
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw ApiException.badRequest("Only orders that are still being processed can be cancelled");
        }
        String note = (reason == null || reason.isBlank())
                ? "Cancelled by customer."
                : "Cancelled by customer: " + reason.trim() + ".";
        cancel(order, note);
        return DtoMapper.toOrderDto(orderRepository.save(order));
    }

    /** Admin moves an order along: Processing -> Shipped -> Delivered, or Processing -> Cancelled. */
    @Transactional
    public OrderResponse updateStatus(Long orderId, String status, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        OrderStatus target = Arrays.stream(OrderStatus.values())
                .filter(s -> s.name().equalsIgnoreCase(status.trim()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("Unknown status: " + status));

        if (!order.getStatus().next().contains(target)) {
            throw ApiException.badRequest("An order that is " + order.getStatus().label()
                    + " cannot be changed to " + target.label());
        }

        String cleanNote = (note == null || note.isBlank()) ? null : note.trim();
        if (target == OrderStatus.CANCELLED) {
            cancel(order, cleanNote != null ? cleanNote : "Cancelled by admin.");
        } else {
            order.setStatus(target);
            String text = cleanNote != null ? cleanNote
                    : (target == OrderStatus.SHIPPED ? "Order shipped." : "Order delivered.");
            // Cash on delivery is settled when the parcel is handed over.
            if (target == OrderStatus.DELIVERED
                    && order.getPaymentMethod() == PaymentMethod.COD
                    && order.getPaymentStatus() == PaymentStatus.PENDING) {
                order.setPaymentStatus(PaymentStatus.PAID);
                text += " Cash payment of \u20b9" + money(order.getTotal()) + " collected.";
            }
            order.addEvent(target, text);
        }
        return DtoMapper.toOrderDto(orderRepository.save(order));
    }

    // ------------------------------------------------------------------ helpers

    /** Puts stock back, returns the coupon use, settles the payment, and records the cancellation. */
    private void cancel(Order order, String note) {
        for (OrderItem item : order.getItems()) {
            productRepository.findByIdForUpdate(item.getProduct().getId())
                    .ifPresent(p -> p.setStock(p.getStock() + item.getQuantity()));
        }
        couponService.release(order.getCouponCode());

        String text = note;
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            text += " Refund of \u20b9" + money(order.getTotal()) + " initiated to your original payment method.";
        } else if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.VOIDED);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.addEvent(OrderStatus.CANCELLED, text);
    }

    private void applyPayment(Order order, PaymentMethod method, CheckoutRequest request, BigDecimal total) {
        order.setPaymentMethod(method);
        if (method == PaymentMethod.CARD) {
            String last4 = last4(request.card());
            if (last4 == null) {
                throw ApiException.badRequest("Enter a valid card number");
            }
            order.setCardLast4(last4);
            order.setPaymentDetail(cardBrand(request.cardBrand()) + " " + DOTS + " " + last4);
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (method == PaymentMethod.UPI) {
            String upi = request.upiId() == null ? "" : request.upiId().trim();
            if (!UPI_PATTERN.matcher(upi).matches()) {
                throw ApiException.badRequest("Enter a valid UPI ID, for example name@bank");
            }
            order.setPaymentDetail(maskUpi(upi));
            order.setPaymentStatus(PaymentStatus.PAID);
        } else {
            if (total.compareTo(codLimit) > 0) {
                throw ApiException.badRequest("Cash on Delivery is available for orders up to \u20b9"
                        + money(codLimit) + ". Please pay online for this order.");
            }
            order.setPaymentDetail("Pay \u20b9" + money(total) + " on delivery");
            order.setPaymentStatus(PaymentStatus.PENDING);
        }
    }

    private static PaymentMethod parseMethod(String raw) {
        return Arrays.stream(PaymentMethod.values())
                .filter(m -> m.name().equalsIgnoreCase(raw == null ? "" : raw.trim()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("Please choose a valid payment method"));
    }

    private static String cardBrand(String brand) {
        return brand != null && CARD_BRANDS.contains(brand) ? brand : "Card";
    }

    /** "rahul.k@okaxis" -> "ra***@okaxis" */
    private static String maskUpi(String upi) {
        int at = upi.indexOf('@');
        String name = upi.substring(0, at);
        String shown = name.length() <= 2 ? name.charAt(0) + "*" : name.substring(0, 2) + "***";
        return shown + upi.substring(at);
    }

    private static String last4(String card) {
        if (card == null) {
            return null;
        }
        String digits = card.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        return digits.length() == 4 ? digits : (digits.length() > 4 ? digits.substring(digits.length() - 4) : null);
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
