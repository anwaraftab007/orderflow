package com.orderflow.orderflow.event;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record OrderEvent(UUID eventId, Long orderId, Long userId, OrderEventType eventType,
                         Instant occurredAt, BigDecimal totalAmount) {
    public static OrderEvent create(Long orderId, Long userId, OrderEventType type, BigDecimal totalAmount) {
        return new OrderEvent(UUID.randomUUID(), orderId, userId, type, Instant.now(), totalAmount);
    }
}
