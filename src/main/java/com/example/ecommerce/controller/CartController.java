package com.example.ecommerce.controller;

import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.CartResponse;
import com.example.ecommerce.dto.UpdateQuantityRequest;
import com.example.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(Principal principal) {
        return cartService.getCart(principal.getName());
    }

    @PostMapping("/items")
    public CartResponse add(Principal principal, @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(principal.getName(), request.productId(), request.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartResponse setQuantity(Principal principal, @PathVariable Long productId,
                                    @Valid @RequestBody UpdateQuantityRequest request) {
        return cartService.setQuantity(principal.getName(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(Principal principal, @PathVariable Long productId) {
        return cartService.removeItem(principal.getName(), productId);
    }

    @DeleteMapping
    public CartResponse clear(Principal principal) {
        return cartService.clear(principal.getName());
    }
}
