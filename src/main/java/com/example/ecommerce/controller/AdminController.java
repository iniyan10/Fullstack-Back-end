package com.example.ecommerce.controller;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Every route under /api/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final DisputeService disputeService;
    private final ProductService productService;
    private final CouponService couponService;
    private final StatsService statsService;

    public AdminController(UserService userService, OrderService orderService, DisputeService disputeService,
                           ProductService productService, CouponService couponService, StatsService statsService) {
        this.userService = userService;
        this.orderService = orderService;
        this.disputeService = disputeService;
        this.productService = productService;
        this.couponService = couponService;
        this.statsService = statsService;
    }

    // ---- dashboard ----

    @GetMapping("/stats")
    public StatsResponse stats() {
        return statsService.overview();
    }

    // ---- products (create/update/delete live on /api/products) ----

    /** Includes archived products, which the public catalogue hides. */
    @GetMapping("/products")
    public List<ProductResponse> products() {
        return productService.listAllForAdmin();
    }

    // ---- coupons ----

    @GetMapping("/coupons")
    public List<CouponResponse> coupons() {
        return couponService.listAll();
    }

    @PostMapping("/coupons")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(request));
    }

    @PutMapping("/coupons/{id}")
    public CouponResponse updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return couponService.update(id, request);
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- users ----

    @GetMapping("/users")
    public List<UserDto> users() {
        return userService.listAll();
    }

    @PatchMapping("/users/{id}/verify")
    public UserDto verify(@PathVariable Long id) {
        return userService.verify(id);
    }

    // ---- orders ----

    @GetMapping("/orders")
    public List<OrderResponse> orders() {
        return orderService.allOrders();
    }

    @PatchMapping("/orders/{id}/status")
    public OrderResponse updateOrderStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status(), request.note());
    }

    // ---- disputes ----

    @GetMapping("/disputes")
    public List<DisputeResponse> disputes() {
        return disputeService.all();
    }

    @PatchMapping("/disputes/{id}/resolve")
    public DisputeResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveDisputeRequest request) {
        return disputeService.resolve(id, request.resolution());
    }
}
