package dev.haritonenko.orders.external.config;

import dev.haritonenko.orders.external.payment_stub.PaymentStubHttpClient;
import dev.haritonenko.orders.external.payment_stub.properties.PaymentStubHttpClientProperties;
import dev.haritonenko.orders.external.warehouse.WarehouseHttpClient;
import dev.haritonenko.orders.external.warehouse.properties.WarehouseHttpClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties({
        PaymentStubHttpClientProperties.class,
        WarehouseHttpClientProperties.class
})
public class ExternalClientsConfig {

    @Bean
    PaymentStubHttpClient paymentStubHttpClient(
            RestClient.Builder builder,
            PaymentStubHttpClientProperties props
    ) {
        RestClient restClient = builder.baseUrl(props.getBaseUrl()).build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(PaymentStubHttpClient.class);
    }

    @Bean
    WarehouseHttpClient warehouseHttpClient(
            RestClient.Builder builder,
            WarehouseHttpClientProperties props
    ) {
        RestClient restClient = builder.baseUrl(props.getBaseUrl()).build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(WarehouseHttpClient.class);
    }
}