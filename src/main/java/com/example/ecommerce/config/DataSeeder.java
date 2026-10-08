package com.example.ecommerce.config;

import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.UserStatus;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/** Inserts the admin account and the watch catalogue the first time the app starts on an empty database. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int DEFAULT_STOCK = 50;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository userRepository,
                      ProductRepository productRepository,
                      CouponRepository couponRepository,
                      PasswordEncoder passwordEncoder,
                      ObjectMapper objectMapper,
                      @Value("${app.admin.name}") String adminName,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) throws Exception {
        seedAdmin();
        seedProducts();
        seedCoupons();
    }

    private void seedCoupons() {
        if (couponRepository.count() > 0) {
            return;
        }
        Coupon welcome = new Coupon();
        welcome.setCode("WELCOME10");
        welcome.setDiscountType(DiscountType.PERCENT);
        welcome.setValue(BigDecimal.valueOf(10));
        welcome.setMaxDiscount(BigDecimal.valueOf(500));
        couponRepository.save(welcome);

        Coupon flat = new Coupon();
        flat.setCode("FLAT200");
        flat.setDiscountType(DiscountType.FIXED);
        flat.setValue(BigDecimal.valueOf(200));
        flat.setMinOrderAmount(BigDecimal.valueOf(2000));
        couponRepository.save(flat);
        log.info("Seeded sample coupons: WELCOME10 (10% off, max 500), FLAT200 (200 off over 2000)");
    }

    private void seedAdmin() {
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.VERIFIED);
        userRepository.save(admin);
        log.info("Seeded admin account: {}", email);
    }

    private void seedProducts() throws Exception {
        if (productRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("seed/products.json").getInputStream()) {
            List<SeedProduct> seeds = objectMapper.readValue(in, new TypeReference<List<SeedProduct>>() {});
            for (SeedProduct s : seeds) {
                Product p = new Product();
                p.setName(s.name());
                p.setDescription(s.description());
                p.setPrice(s.price());
                p.setCategory(s.category());
                p.setType(s.type());
                p.setRating(s.rating() == null ? 0 : s.rating());
                p.setReviews(s.reviews() == null ? 0 : s.reviews());
                p.setImage(s.image());
                p.setStock(DEFAULT_STOCK);
                productRepository.save(p);
            }
            log.info("Seeded {} products", seeds.size());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedProduct(String name, String description, BigDecimal price, String category,
                              String type, Double rating, Integer reviews, String image) {}
}
