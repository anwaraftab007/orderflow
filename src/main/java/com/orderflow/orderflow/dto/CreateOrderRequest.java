package com.orderflow.orderflow.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record CreateOrderRequest(@NotEmpty List<@Valid Item> items) {
    public record Item(@NotNull Long productId, @NotNull @Positive Integer quantity) {}
}
