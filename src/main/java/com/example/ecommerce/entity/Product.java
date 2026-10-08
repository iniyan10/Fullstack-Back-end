package com.example.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** "male" | "female" */
    @Column(nullable = false, length = 50)
    private String category;

    /** "smart" | "sports" | "formal" */
    @Column(length = 50)
    private String type;

    @Column(nullable = false)
    private double rating = 0;

    @Column(nullable = false)
    private int reviews = 0;

    /** URL or base64 data-URI (the existing catalogue contains one data-URI image). */
    @Column(columnDefinition = "LONGTEXT")
    private String image;

    @Column(nullable = false)
    private int stock = 0;

    /** false = archived: hidden from the shop but kept so old orders still reference it. */
    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private boolean active = true;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getReviews() { return reviews; }
    public void setReviews(int reviews) { this.reviews = reviews; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
