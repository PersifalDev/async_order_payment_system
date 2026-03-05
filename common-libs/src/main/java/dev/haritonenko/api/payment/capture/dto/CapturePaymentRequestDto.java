package dev.haritonenko.api.payment.capture.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CapturePaymentRequestDto(
        BigDecimal captureAmount,
        Long customerId
) {}
