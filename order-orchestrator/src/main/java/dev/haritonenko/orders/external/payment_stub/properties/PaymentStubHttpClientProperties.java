package dev.haritonenko.orders.external.payment_stub.properties;

import dev.haritonenko.orders.external.config.properties.HttpClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients.payment")
public class PaymentStubHttpClientProperties extends HttpClientProperties {
}
