package com.example.ecommerce.controller;

import com.example.ecommerce.dto.CancelOrderRequest;
import com.example.ecommerce.dto.CheckoutRequest;
import com.example.ecommerce.dto.DisputeRequest;
import com.example.ecommerce.dto.DisputeResponse;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.service.DisputeService;
import com.example.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final DisputeService disputeService;

    public OrderController(OrderService orderService, DisputeService disputeService) {
        this.orderService = orderService;
        this.disputeService = disputeService;
    }

    /** The logged-in user's order history. */
    @GetMapping
    public List<OrderResponse> myOrders(Principal principal) {
        return orderService.myOrders(principal.getName());
    }

    /** Checkout: orders the whole cart, or only the given items for "Buy now". Paid by card, UPI or COD. */
    @PostMapping
    public ResponseEntity<OrderResponse> place(Principal principal, @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(principal.getName(), request));
    }

    /** One of my orders (used by the tracking page). Other people's orders are a 404. */
    @GetMapping("/{orderId}")
    public OrderResponse get(Principal principal, @PathVariable Long orderId) {
        return orderService.getMine(principal.getName(), orderId);
    }

    /** Customer cancels their own order (only while it is still Processing). Stock is returned. */
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(Principal principal, @PathVariable Long orderId,
                                @RequestBody(required = false) CancelOrderRequest request) {
        return orderService.cancelMine(principal.getName(), orderId, request == null ? null : request.reason());
    }

    /** Customer raises a dispute on one of their own orders. */
    @PostMapping("/{orderId}/disputes")
    public ResponseEntity<DisputeResponse> raiseDispute(Principal principal, @PathVariable Long orderId,
                                                        @Valid @RequestBody DisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disputeService.create(principal.getName(), orderId, request));
    }

    /** The logged-in user's disputes. */
    @GetMapping("/disputes")
    public List<DisputeResponse> myDisputes(Principal principal) {
        return disputeService.mine(principal.getName());
    }
}
