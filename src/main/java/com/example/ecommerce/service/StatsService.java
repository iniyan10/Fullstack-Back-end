package com.example.ecommerce.service;

import com.example.ecommerce.dto.StatsResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class StatsService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DisputeRepository disputeRepository;

    public StatsService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        UserRepository userRepository, ProductRepository productRepository,
                        DisputeRepository disputeRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.disputeRepository = disputeRepository;
    }

    @Transactional(readOnly = true)
    public StatsResponse overview() {
        BigDecimal revenue = orderRepository.sumTotalExcluding(OrderStatus.CANCELLED);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }
        long orders = orderRepository.countByStatusNot(OrderStatus.CANCELLED);
        BigDecimal average = orders == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status.label(), orderRepository.countByStatus(status));
        }

        List<StatsResponse.TopProduct> top = orderItemRepository
                .topSelling(OrderStatus.CANCELLED, PageRequest.of(0, 5)).stream()
                .map(r -> new StatsResponse.TopProduct(
                        ((Number) r[0]).longValue(),
                        String.valueOf(r[1]),
                        ((Number) r[2]).longValue(),
                        new BigDecimal(String.valueOf(r[3]))))
                .toList();

        List<StatsResponse.LowStockProduct> lowStock = productRepository
                .findTop10ByActiveTrueAndStockLessThanEqualOrderByStockAsc(LOW_STOCK_THRESHOLD).stream()
                .map(p -> new StatsResponse.LowStockProduct(p.getId(), p.getName(), p.getStock()))
                .toList();

        return new StatsResponse(
                revenue, orders, average, byStatus,
                userRepository.countByRole(Role.USER),
                userRepository.countByRoleAndStatus(Role.USER, UserStatus.PENDING),
                productRepository.countByActiveTrue(),
                disputeRepository.countByStatus(DisputeStatus.OPEN),
                last7Days(), top, lowStock);
    }

    /** One row per day for the last 7 days (UTC), including days with no sales. */
    private List<StatsResponse.DailySales> last7Days() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(6);
        List<Order> recent = orderRepository.findByCreatedAtGreaterThanEqualAndStatusNot(
                firstDay.atStartOfDay().toInstant(ZoneOffset.UTC), OrderStatus.CANCELLED);

        Map<LocalDate, BigDecimal> revenueByDay = new HashMap<>();
        Map<LocalDate, Long> ordersByDay = new HashMap<>();
        for (Order order : recent) {
            LocalDate day = order.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            revenueByDay.merge(day, order.getTotal(), BigDecimal::add);
            ordersByDay.merge(day, 1L, Long::sum);
        }

        List<StatsResponse.DailySales> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = firstDay.plusDays(i);
            result.add(new StatsResponse.DailySales(day.toString(),
                    revenueByDay.getOrDefault(day, BigDecimal.ZERO),
                    ordersByDay.getOrDefault(day, 0L)));
        }
        return result;
    }
}
