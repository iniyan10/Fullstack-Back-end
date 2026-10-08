package com.example.ecommerce.controller;

import com.example.ecommerce.dto.DeleteProductResponse;
import com.example.ecommerce.dto.PageResponse;
import com.example.ecommerce.dto.ProductRequest;
import com.example.ecommerce.dto.ProductResponse;
import com.example.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ---- public ----

    /** All active products (used by the home page and product detail page). */
    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String category,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String q) {
        return productService.list(category, type, q);
    }

    /**
     * Server-side search with filters, sorting and pagination.
     * sort = price_asc | price_desc | rating | popular | newest | name
     */
    @GetMapping("/search")
    public PageResponse<ProductResponse> search(@RequestParam(required = false) String category,
                                                @RequestParam(required = false) String type,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(required = false) String sort,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "12") int size) {
        return productService.search(category, type, q, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    // ---- admin only (enforced in SecurityConfig) ----

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    /** Deletes the product, or archives it if it appears in past orders. */
    @DeleteMapping("/{id}")
    public DeleteProductResponse delete(@PathVariable Long id) {
        return productService.delete(id);
    }
}
