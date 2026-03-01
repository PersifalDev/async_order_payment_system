package dev.haritonenko.payment.api.controller;

import dev.haritonenko.api.payment.authorization.AuthorizationStatus;
import dev.haritonenko.api.payment.authorization.dto.AuthorizePaymentRequestDto;
import dev.haritonenko.api.payment.authorization.dto.AuthorizePaymentResponseDto;
import dev.haritonenko.api.payment.capture.dto.CapturePaymentRequestDto;
import dev.haritonenko.api.payment.capture.dto.CapturePaymentResponseDto;
import dev.haritonenko.api.payment.capture.CaptureStatus;
import dev.haritonenko.payment.config.PaymentStubProperties;
import dev.haritonenko.utils.StubUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP-stub платежного шлюза: авторизация и списание
 */
@Slf4j
@RestController
@RequestMapping("/payment")
@AllArgsConstructor
public class PaymentStubController {

    private final PaymentStubProperties properties;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizePaymentResponseDto> authorizePayment(
            @RequestBody AuthorizePaymentRequestDto authorizePaymentRequest
    ) {
        log.info("Authorize called: request={}", authorizePaymentRequest);
        StubUtils.randomSafeSleepMs(
                properties.getAuthorizeLatencyMinMillis(),
                properties.getAuthorizeLatencyMaxMillis()
        );

        var isThrowException = properties.isExceptionEnabled()
                && StubUtils.chance(properties.getExceptionProbability());

        if (isThrowException) {
            throw new RuntimeException("Payment gateway unavailable (simulated)");
        }

        boolean isAuthorizeDeclined = properties.isDeclineEnabled()
                && StubUtils.chance(properties.getDeclineProbability());

        if (isAuthorizeDeclined) {
            log.info("Authorize declined");
            return ResponseEntity.ok(
                    new AuthorizePaymentResponseDto(
                            null,
                            authorizePaymentRequest.amount(),
                            AuthorizationStatus.DECLINED,
                            "Card was declined by stub"
                    )
            );
        }

        var authId = UUID.randomUUID();

        log.info("Authorize approved: authId={}", authId);
        return ResponseEntity.ok(
                new AuthorizePaymentResponseDto(
                        authId,
                        authorizePaymentRequest.amount(),
                        AuthorizationStatus.AUTHORIZED,
                        null
                )
        );
    }

    @PostMapping("/capture")
    public ResponseEntity<CapturePaymentResponseDto> capturePayment(
            @RequestBody CapturePaymentRequestDto capturePaymentRequest
    ) {
        log.info("Capture called: request={}", capturePaymentRequest);
        StubUtils.randomSafeSleepMs(
                properties.getCaptureLatencyMinMillis(),
                properties.getCaptureLatencyMaxMillis()
        );

        var isThrowException = properties.isExceptionEnabled()
                && StubUtils.chance(properties.getExceptionProbability());

        if (isThrowException) {
            throw new RuntimeException("Payment gateway capture failed (simulated)");
        }

        var captureId = UUID.randomUUID();

        log.info("Capture succeeded: captureId={}", captureId);

        return ResponseEntity.ok(
                new CapturePaymentResponseDto(
                        captureId,
                        capturePaymentRequest.captureAmount(),
                        CaptureStatus.CAPTURED,
                        null
                )
        );
    }
}
