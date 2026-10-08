package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.CouponRequest;
import com.example.ecommerce.dto.CouponResponse;
import com.example.ecommerce.dto.CouponValidationResponse;
import com.example.ecommerce.dto.DtoMapper;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public CouponService(CouponRepository couponRepository, OrderRepository orderRepository,
                         CartRepository cartRepository, ProductRepository productRepository,
                         UserService userService) {
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    // ------------------------------------------------------------------ customer

    /**
     * Previews what a coupon would do to the user's cart, or to the given "buy now" items when
     * {@code items} is not empty. Does not consume the coupon.
     */
    @Transactional
    public CouponValidationResponse preview(String email, String code, List<CartItemRequest> items) {
        User user = userService.getByEmail(email);
        BigDecimal subtotal;
        if (items != null && !items.isEmpty()) {
            subtotal = BigDecimal.ZERO;
            for (CartItemRequest item : items) {
                if (item.quantity() <= 0) {
                    throw ApiException.badRequest("Quantity must be at least 1");
                }
                Product product = productRepository.findById(item.productId())
                        .filter(Product::isActive)
                        .orElseThrow(() -> ApiException.notFound("Product not found"));
                subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
            }
        } else {
            Cart cart = cartRepository.findByUserId(user.getId())
                    .orElseThrow(() -> ApiException.badRequest("Your cart is empty"));
            subtotal = cart.getItems().stream()
                    .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (subtotal.signum() == 0) {
            throw ApiException.badRequest("Your cart is empty");
        }
        Coupon coupon = requireUsable(code, subtotal, user);
        BigDecimal discount = coupon.computeDiscount(subtotal);
        return new CouponValidationResponse(coupon.getCode(), describe(coupon), subtotal, discount,
                subtotal.subtract(discount));
    }

    /**
     * Loads the coupon with a row lock and checks every rule. Used both by the preview and by
     * checkout (which then increments usedCount inside the same transaction).
     */
    @Transactional
    public Coupon requireUsable(String rawCode, BigDecimal subtotal, User user) {
        String code = rawCode.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> ApiException.badRequest("Invalid coupon code"));
        if (!coupon.isActive()) {
            throw ApiException.badRequest("This coupon is not active");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("This coupon has expired");
        }
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw ApiException.badRequest("This coupon has reached its usage limit");
        }
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw ApiException.badRequest("This coupon needs a minimum order of ₹"
                    + coupon.getMinOrderAmount().stripTrailingZeros().toPlainString());
        }
        if (orderRepository.existsByUserIdAndCouponCodeIgnoreCaseAndStatusNot(user.getId(), code, OrderStatus.CANCELLED)) {
            throw ApiException.badRequest("You have already used this coupon");
        }
        return coupon;
    }

    /** Gives a coupon use back (order cancelled). */
    @Transactional
    public void release(String code) {
        if (code == null) {
            return;
        }
        couponRepository.findByCodeForUpdate(code).ifPresent(c -> {
            if (c.getUsedCount() > 0) {
                c.setUsedCount(c.getUsedCount() - 1);
            }
        });
    }

    // ------------------------------------------------------------------ admin CRUD

    @Transactional(readOnly = true)
    public List<CouponResponse> listAll() {
        return couponRepository.findAllByOrderByCreatedAtDesc().stream().map(DtoMapper::toCouponDto).toList();
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = request.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("Coupon code already exists");
        }
        Coupon coupon = new Coupon();
        apply(coupon, request, code);
        return DtoMapper.toCouponDto(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Coupon not found"));
        String code = request.code().trim().toUpperCase();
        if (!coupon.getCode().equalsIgnoreCase(code) && couponRepository.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("Coupon code already exists");
        }
        apply(coupon, request, code);
        return DtoMapper.toCouponDto(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Coupon not found"));
        couponRepository.delete(coupon);
    }

    private void apply(Coupon c, CouponRequest r, String code) {
        if (r.discountType() == DiscountType.PERCENT && r.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest("A percentage discount cannot be more than 100");
        }
        c.setCode(code);
        c.setDiscountType(r.discountType());
        c.setValue(r.value());
        c.setMinOrderAmount(r.minOrderAmount() == null ? BigDecimal.ZERO : r.minOrderAmount());
        c.setMaxDiscount(r.discountType() == DiscountType.PERCENT ? r.maxDiscount() : null);
        c.setMaxUses(r.maxUses());
        c.setExpiresAt(r.expiresAt());
        if (r.active() != null) {
            c.setActive(r.active());
        }
    }

    private static String describe(Coupon c) {
        String value = c.getValue().stripTrailingZeros().toPlainString();
        return c.getDiscountType() == DiscountType.PERCENT ? value + "% off" : "₹" + value + " off";
    }
}
