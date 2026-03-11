package dev.haritonenko.orders.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderCreateRequestDto(
        @NotBlank(message = "Address can't be blank")
        String address,
        @NotNull(message = "Client sum can't be blank")
        @DecimalMin(value = "0.01", message = "Minimum value for client sum is 0.01")
        BigDecimal clientEstimate
) {
}
