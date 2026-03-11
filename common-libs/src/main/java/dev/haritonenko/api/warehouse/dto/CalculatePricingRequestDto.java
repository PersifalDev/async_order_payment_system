package dev.haritonenko.api.warehouse.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CalculatePricingRequestDto (
        UUID orderId
) { }
