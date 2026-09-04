package com.orderflow.orderflow.dto;
import com.orderflow.orderflow.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record OrderResponse(Long orderId, OrderStatus status, BigDecimal totalAmount, Instant createdAt, List<Item> items) {
    public record Item(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
}
