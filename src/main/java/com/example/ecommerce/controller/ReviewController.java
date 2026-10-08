package com.example.ecommerce.controller;

import com.example.ecommerce.dto.ReviewRequest;
import com.example.ecommerce.dto.ReviewResponse;
import com.example.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Public: GET /api/products/** is open in SecurityConfig. */
    @GetMapping("/products/{productId}/reviews")
    public List<ReviewResponse> list(@PathVariable Long productId) {
        return reviewService.list(productId);
    }

    /** Creates the caller's review, or edits it if they already reviewed this product. */
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ReviewResponse> upsert(Authentication authentication, @PathVariable Long productId,
                                                 @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(reviewService.upsert(authentication.getName(), productId, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long reviewId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        reviewService.delete(authentication.getName(), reviewId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
