package dev.haritonenko.orders.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.haritonenko.orders.domain.status.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record OrderDto(
        UUID id,
        String address,
        BigDecimal clientEstimate,
        BigDecimal finalAmount,
        BigDecimal capturedAmount,
        PaymentStatus paymentStatus,
        String cancellationReason
) {
}
