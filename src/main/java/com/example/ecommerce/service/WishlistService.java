package com.example.ecommerce.service;

import com.example.ecommerce.dto.DtoMapper;
import com.example.ecommerce.dto.ProductResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.WishlistItem;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository,
                           UserService userService) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(String email) {
        User user = userService.getByEmail(email);
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(WishlistItem::getProduct)
                .filter(Product::isActive)
                .map(DtoMapper::toProductDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> ids(String email) {
        User user = userService.getByEmail(email);
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(item -> item.getProduct().getId())
                .toList();
    }

    /** Idempotent: adding a product that is already saved is a no-op. */
    @Transactional
    public List<Long> add(String email, Long productId) {
        User user = userService.getByEmail(email);
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (wishlistRepository.findByUserIdAndProductId(user.getId(), productId).isEmpty()) {
            wishlistRepository.save(new WishlistItem(user, product));
        }
        return ids(email);
    }

    @Transactional
    public List<Long> remove(String email, Long productId) {
        User user = userService.getByEmail(email);
        wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresent(wishlistRepository::delete);
        return ids(email);
    }
}
