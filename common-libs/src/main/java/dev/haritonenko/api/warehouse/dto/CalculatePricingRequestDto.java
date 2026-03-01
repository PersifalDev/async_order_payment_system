package dev.haritonenko.api.warehouse.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Запрос на пересчёт цены заказа по его идентификатору
 */
@Builder
public record CalculatePricingRequestDto (
        UUID orderId
) { }
