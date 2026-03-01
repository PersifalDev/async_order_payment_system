package dev.haritonenko.api.payment.authorization.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Запрос на авторизацию карты на указанную сумму
 */
@Builder
public record AuthorizePaymentRequestDto(
    Long customerId,
    BigDecimal amount
) { }
