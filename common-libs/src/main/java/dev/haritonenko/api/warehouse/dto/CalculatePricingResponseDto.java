package dev.haritonenko.api.warehouse.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CalculatePricingResponseDto(
        UUID orderId,
        BigDecimal finalAmount,
        String reason
) { }
