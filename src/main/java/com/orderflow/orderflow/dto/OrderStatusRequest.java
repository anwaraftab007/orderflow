package com.orderflow.orderflow.dto;
import com.orderflow.orderflow.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
public record OrderStatusRequest(@NotNull OrderStatus status) {}
