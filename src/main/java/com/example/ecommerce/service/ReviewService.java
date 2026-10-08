package com.example.ecommerce.service;

import com.example.ecommerce.dto.DtoMapper;
import com.example.ecommerce.dto.ReviewRequest;
import com.example.ecommerce.dto.ReviewResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         OrderItemRepository orderItemRepository, UserService userService) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> list(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw ApiException.notFound("Product not found");
        }
        Set<Long> buyers = new HashSet<>(orderItemRepository.findBuyerIds(productId, OrderStatus.CANCELLED));
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(r -> DtoMapper.toReviewDto(r, buyers.contains(r.getUser().getId())))
                .toList();
    }

    /** Creates the user's review, or edits it if they already reviewed this product. */
    @Transactional
    public ReviewResponse upsert(String email, Long productId, ReviewRequest request) {
        User user = userService.getByEmail(email);
        // Lock the product row: the rating average is updated incrementally below.
        Product product = productRepository.findByIdForUpdate(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        String comment = request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
        int stars = request.rating();

        Review review = reviewRepository.findByProductIdAndUserId(productId, user.getId()).orElse(null);
        if (review == null) {
            review = new Review();
            review.setProduct(product);
            review.setUser(user);
            review.setRating(stars);
            review.setComment(comment);
            applyAdd(product, stars);
        } else {
            applyChange(product, review.getRating(), stars);
            review.setRating(stars);
            review.setComment(comment);
            review.setUpdatedAt(Instant.now());
        }
        Review saved = reviewRepository.save(review);
        productRepository.save(product);

        boolean verified = orderItemRepository.findBuyerIds(productId, OrderStatus.CANCELLED).contains(user.getId());
        return DtoMapper.toReviewDto(saved, verified);
    }

    /** The author can delete their own review; admins can delete any. */
    @Transactional
    public void delete(String email, Long reviewId, boolean isAdmin) {
        User user = userService.getByEmail(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));
        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            throw ApiException.notFound("Review not found");
        }
        Product product = productRepository.findByIdForUpdate(review.getProduct().getId())
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        applyRemove(product, review.getRating());
        reviewRepository.delete(review);
        productRepository.save(product);
    }

    // ---- incremental rating average (keeps the catalogue's existing seed ratings meaningful) ----

    private static void applyAdd(Product p, int stars) {
        int n = p.getReviews();
        double sum = p.getRating() * n + stars;
        p.setReviews(n + 1);
        p.setRating(round1(sum / (n + 1)));
    }

    private static void applyChange(Product p, int oldStars, int newStars) {
        int n = p.getReviews();
        if (n <= 0) {
            p.setReviews(1);
            p.setRating(newStars);
            return;
        }
        double sum = p.getRating() * n - oldStars + newStars;
        p.setRating(clamp(round1(sum / n)));
    }

    private static void applyRemove(Product p, int stars) {
        int n = p.getReviews();
        if (n <= 1) {
            p.setReviews(0);
            p.setRating(0);
            return;
        }
        double sum = p.getRating() * n - stars;
        p.setReviews(n - 1);
        p.setRating(clamp(round1(sum / (n - 1))));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(5, v));
    }
}
