package com.example.ecommerce.repository;

import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);

    /** Rows of [productId, productName, unitsSold, revenue], best sellers first. */
    @Query("SELECT oi.product.id, oi.productName, SUM(oi.quantity), SUM(oi.unitPrice * oi.quantity) "
            + "FROM OrderItem oi WHERE oi.order.status <> :status "
            + "GROUP BY oi.product.id, oi.productName "
            + "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> topSelling(@Param("status") OrderStatus status, Pageable pageable);

    /** Ids of users who bought the product (used for the "verified purchase" badge on reviews). */
    @Query("SELECT DISTINCT oi.order.user.id FROM OrderItem oi "
            + "WHERE oi.product.id = :productId AND oi.order.status <> :status")
    List<Long> findBuyerIds(@Param("productId") Long productId, @Param("status") OrderStatus status);
}
