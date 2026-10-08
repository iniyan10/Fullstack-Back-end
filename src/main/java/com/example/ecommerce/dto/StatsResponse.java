package com.example.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Everything the admin overview needs in a single call. Revenue and order counts exclude cancelled orders. */
public record StatsResponse(
        BigDecimal totalRevenue,
        long totalOrders,
        BigDecimal averageOrderValue,
        Map<String, Long> ordersByStatus,
        long totalCustomers,
        long pendingUsers,
        long totalProducts,
        long openDisputes,
        List<DailySales> last7Days,
        List<TopProduct> topProducts,
        List<LowStockProduct> lowStock
) {
    public record DailySales(String date, BigDecimal revenue, long orders) {}

    public record TopProduct(Long productId, String name, long unitsSold, BigDecimal revenue) {}

    public record LowStockProduct(Long id, String name, int stock) {}
}
