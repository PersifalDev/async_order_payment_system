package dev.haritonenko.orders.external.warehouse;

import dev.haritonenko.api.warehouse.dto.CalculatePricingRequestDto;
import dev.haritonenko.api.warehouse.dto.CalculatePricingResponseDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface WarehouseHttpClient {

    @PostExchange("/calculate-price")
    CalculatePricingResponseDto calculatePricing(
            @RequestBody CalculatePricingRequestDto captureRequest);

}
