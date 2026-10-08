package com.example.ecommerce.controller;

import com.example.ecommerce.dto.PaymentOptionsResponse;
import com.example.ecommerce.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final OrderService orderService;

    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Lets the checkout page know the Cash on Delivery limit. */
    @GetMapping("/options")
    public PaymentOptionsResponse options() {
        return new PaymentOptionsResponse(orderService.getCodLimit());
    }
}
