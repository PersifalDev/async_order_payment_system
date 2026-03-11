package dev.haritonenko.api.payment.authorization.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AuthorizePaymentRequestDto(
    Long customerId,
    BigDecimal amount
) { }
