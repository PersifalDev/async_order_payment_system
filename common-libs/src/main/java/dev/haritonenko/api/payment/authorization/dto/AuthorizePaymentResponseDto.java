package dev.haritonenko.api.payment.authorization.dto;

import dev.haritonenko.api.payment.authorization.AuthorizationStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ответ авторизации: идентификатор, сумма и итоговый статус
 */
public record AuthorizePaymentResponseDto(
        UUID authorizationId,
        BigDecimal authorizedAmount,
        AuthorizationStatus status,
        String message
) { }
