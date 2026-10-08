package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartResponse;
import com.example.ecommerce.dto.DtoMapper;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public CartService(CartRepository cartRepository, ProductRepository productRepository, UserService userService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional
    public CartResponse getCart(String email) {
        return DtoMapper.toCartDto(getOrCreateCart(userService.getByEmail(email)));
    }

    /** Adds {@code quantity} of a product (or increases the existing line). */
    @Transactional
    public CartResponse addItem(String email, Long productId, int quantity) {
        if (quantity <= 0) {
            throw ApiException.badRequest("Quantity must be at least 1");
        }
        Cart cart = getOrCreateCart(userService.getByEmail(email));
        Product product = findProduct(productId);

        CartItem line = findLine(cart, productId);
        int newQuantity = (line == null ? 0 : line.getQuantity()) + quantity;
        ensureStock(product, newQuantity);

        if (line == null) {
            cart.getItems().add(new CartItem(cart, product, newQuantity));
        } else {
            line.setQuantity(newQuantity);
        }
        return DtoMapper.toCartDto(cartRepository.save(cart));
    }

    /** Sets the quantity of a line. Quantity 0 or less removes it. */
    @Transactional
    public CartResponse setQuantity(String email, Long productId, int quantity) {
        if (quantity <= 0) {
            return removeItem(email, productId);
        }
        Cart cart = getOrCreateCart(userService.getByEmail(email));
        CartItem line = findLine(cart, productId);
        if (line == null) {
            throw ApiException.notFound("Item is not in the cart");
        }
        ensureStock(line.getProduct(), quantity);
        line.setQuantity(quantity);
        return DtoMapper.toCartDto(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(String email, Long productId) {
        Cart cart = getOrCreateCart(userService.getByEmail(email));
        cart.getItems().removeIf(ci -> ci.getProduct().getId().equals(productId));
        return DtoMapper.toCartDto(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clear(String email) {
        Cart cart = getOrCreateCart(userService.getByEmail(email));
        cart.getItems().clear();
        return DtoMapper.toCartDto(cartRepository.save(cart));
    }

    // ---- helpers ----

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    private CartItem findLine(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void ensureStock(Product product, int wanted) {
        if (wanted > product.getStock()) {
            throw ApiException.badRequest("Only " + product.getStock() + " unit(s) of \"" + product.getName() + "\" in stock");
        }
    }
}
