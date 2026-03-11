package dev.haritonenko.orders.external.config.properties;

import lombok.Data;

import java.time.Duration;

@Data
public abstract class HttpClientProperties {
    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;
}
