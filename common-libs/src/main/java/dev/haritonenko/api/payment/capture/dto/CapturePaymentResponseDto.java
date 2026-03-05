package dev.haritonenko.api.payment.capture.dto;

import dev.haritonenko.api.payment.capture.CaptureStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CapturePaymentResponseDto(
        UUID captureId,
        BigDecimal capturedAmount,
        CaptureStatus status,
        String message
) {}
