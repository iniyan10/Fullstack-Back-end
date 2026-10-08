package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findAllByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);
    long countByStatusNot(OrderStatus status);

    /** Returns null when there are no matching orders. */
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status <> :status")
    BigDecimal sumTotalExcluding(@Param("status") OrderStatus status);

    List<Order> findByCreatedAtGreaterThanEqualAndStatusNot(Instant since, OrderStatus status);

    boolean existsByUserIdAndCouponCodeIgnoreCaseAndStatusNot(Long userId, String couponCode, OrderStatus status);
}
