package dev.haritonenko.api.payment.capture.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Запрос на списание (capture) по ранее авторизованной карте
 */

@Builder
public record CapturePaymentRequestDto(
        BigDecimal captureAmount,
        Long customerId
) {}
