package com.example.ecommerce.service;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;

    public ProductService(ProductRepository productRepository,
                          OrderItemRepository orderItemRepository,
                          CartItemRepository cartItemRepository,
                          WishlistRepository wishlistRepository,
                          ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistRepository = wishlistRepository;
        this.reviewRepository = reviewRepository;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ------------------------------------------------------------------ public reads

    /** All active products (the storefront home page uses this). */
    @Transactional(readOnly = true)
    public List<ProductResponse> list(String category, String type, String q) {
        return productRepository.findAll(filter(category, type, q, null, null), Sort.by("id"))
                .stream().map(DtoMapper::toProductDto).toList();
    }

    /** Filtered, sorted, paginated catalogue search. */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String category, String type, String q,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                String sort, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 48);
        int safePage = Math.max(page, 0);
        Page<Product> result = productRepository.findAll(
                filter(category, type, q, minPrice, maxPrice),
                PageRequest.of(safePage, safeSize, sortFor(sort)));
        return new PageResponse<>(
                result.getContent().stream().map(DtoMapper::toProductDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product product = find(id);
        if (!product.isActive()) {
            throw ApiException.notFound("Product not found");
        }
        return DtoMapper.toProductDto(product);
    }

    // ------------------------------------------------------------------ admin

    /** Every product including archived ones. */
    @Transactional(readOnly = true)
    public List<ProductResponse> listAllForAdmin() {
        return productRepository.findAll(Sort.by("id")).stream().map(DtoMapper::toProductDto).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return DtoMapper.toProductDto(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        return DtoMapper.toProductDto(productRepository.save(product));
    }

    /**
     * Removes a product. If it already appears in past orders it cannot be deleted without
     * corrupting order history, so it is archived (hidden from the shop) instead.
     */
    @Transactional
    public DeleteProductResponse delete(Long id) {
        Product product = find(id);

        // Either way it must disappear from carts and wishlists.
        cartItemRepository.deleteByProductId(id);
        wishlistRepository.deleteByProductId(id);

        if (orderItemRepository.existsByProductId(id)) {
            product.setActive(false);
            productRepository.save(product);
            return new DeleteProductResponse(true,
                    "This product is part of existing orders, so it was archived (hidden from the shop) instead of deleted.");
        }
        reviewRepository.deleteByProductId(id);
        productRepository.delete(product);
        return new DeleteProductResponse(false, "Product deleted.");
    }

    // ------------------------------------------------------------------ helpers

    private Product find(Long id) {
        return productRepository.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setCategory(r.category().trim().toLowerCase());
        p.setType(r.type() == null || r.type().isBlank() ? null : r.type().trim().toLowerCase());
        p.setImage(r.image());
        p.setStock(r.stock() == null ? 0 : r.stock());
        if (r.active() != null) {
            p.setActive(r.active());
        }
    }

    private static Specification<Product> filter(String category, String type, String q,
                                                 BigDecimal minPrice, BigDecimal maxPrice) {
        String cat = blankToNull(category);
        String typ = blankToNull(type);
        String term = blankToNull(q);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (cat != null) {
                predicates.add(cb.equal(root.get("category"), cat));
            }
            if (typ != null) {
                predicates.add(cb.equal(root.get("type"), typ));
            }
            if (term != null) {
                String like = "%" + term.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Sort sortFor(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Order.asc("id"));
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case "rating" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("reviews"), Sort.Order.asc("id"));
            case "popular" -> Sort.by(Sort.Order.desc("reviews"), Sort.Order.asc("id"));
            case "newest" -> Sort.by(Sort.Order.desc("id"));
            case "name" -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.asc("id"));
        };
    }
}
