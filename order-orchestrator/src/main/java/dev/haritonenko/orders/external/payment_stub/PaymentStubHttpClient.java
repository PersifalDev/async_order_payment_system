package dev.haritonenko.orders.external.payment_stub;

import dev.haritonenko.api.payment.authorization.dto.AuthorizePaymentRequestDto;
import dev.haritonenko.api.payment.authorization.dto.AuthorizePaymentResponseDto;
import dev.haritonenko.api.payment.capture.dto.CapturePaymentRequestDto;
import dev.haritonenko.api.payment.capture.dto.CapturePaymentResponseDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface PaymentStubHttpClient {

    @PostExchange("/authorize")
    AuthorizePaymentResponseDto authorizePayment(
            @RequestBody AuthorizePaymentRequestDto authorizeRequest);

    @PostExchange("/capture")
    CapturePaymentResponseDto capturePayment(
            @RequestBody CapturePaymentRequestDto captureRequest);


}
