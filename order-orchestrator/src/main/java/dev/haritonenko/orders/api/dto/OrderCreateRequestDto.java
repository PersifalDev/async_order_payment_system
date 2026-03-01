package dev.haritonenko.orders.api.dto;

import java.math.BigDecimal;

public record OrderCreateRequestDto(
        String address,
        BigDecimal clientEstimate
) {
}
