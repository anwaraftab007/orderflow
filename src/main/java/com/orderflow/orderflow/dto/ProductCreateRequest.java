package com.orderflow.orderflow.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ProductCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotNull @PositiveOrZero Integer initialQuantity) {}
