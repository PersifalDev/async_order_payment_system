package dev.haritonenko.orders.api.controller;

import dev.haritonenko.orders.api.dto.OrderCreateRequestDto;
import dev.haritonenko.orders.api.dto.OrderDto;
import dev.haritonenko.orders.domain.db.entity.OrderEntity;
import dev.haritonenko.orders.domain.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
           @Valid @RequestBody OrderCreateRequestDto orderCreateRequestDto
    ) {
        log.info("Received request to create order: request={}", orderCreateRequestDto);
        var created = orderService.createOrder(orderCreateRequestDto);
        log.info("Created order: created={}", created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapEntityToDto(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable UUID id
    ) {
        log.info("Getting order with id {}", id);
        var foundOrder = orderService.findOrder(id);
        return foundOrder
                .map(this::mapEntityToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private OrderDto mapEntityToDto(OrderEntity order) {
        return OrderDto.builder()
                .id(order.getId())
                .address(order.getAddress())
                .paymentStatus(order.getPaymentStatus())
                .finalAmount(order.getFinalAmount())
                .capturedAmount(order.getCapturedAmount())
                .clientEstimate(order.getClientEstimate())
                .cancellationReason(order.getCancellationReason())
                .build();
    }
}
