package dev.haritonenko.orders.external.warehouse.properties;

import dev.haritonenko.orders.external.config.properties.HttpClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients.warehouse")
public class WarehouseHttpClientProperties extends HttpClientProperties {
}