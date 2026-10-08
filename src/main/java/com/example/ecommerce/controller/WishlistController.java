package com.example.ecommerce.controller;

import com.example.ecommerce.dto.ProductResponse;
import com.example.ecommerce.service.WishlistService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /** Full products (for the wishlist page). */
    @GetMapping
    public List<ProductResponse> list(Principal principal) {
        return wishlistService.list(principal.getName());
    }

    /** Just the product ids (cheap; used to colour the heart buttons). */
    @GetMapping("/ids")
    public List<Long> ids(Principal principal) {
        return wishlistService.ids(principal.getName());
    }

    /** Returns the updated list of ids. */
    @PostMapping("/{productId}")
    public List<Long> add(Principal principal, @PathVariable Long productId) {
        return wishlistService.add(principal.getName(), productId);
    }

    /** Returns the updated list of ids. */
    @DeleteMapping("/{productId}")
    public List<Long> remove(Principal principal, @PathVariable Long productId) {
        return wishlistService.remove(principal.getName(), productId);
    }
}
