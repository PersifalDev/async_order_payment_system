package dev.haritonenko.orders.domain.service;

import dev.haritonenko.orders.api.dto.OrderCreateRequestDto;
import dev.haritonenko.orders.domain.status.PaymentStatus;
import dev.haritonenko.orders.domain.db.entity.OrderEntity;
import dev.haritonenko.orders.domain.db.repository.OrderJpaRepository;
import dev.haritonenko.tasks.domain.async.status.ProcessingStep;
import dev.haritonenko.tasks.domain.async.db.entity.AsyncPaymentTaskEntity;
import dev.haritonenko.tasks.domain.async.db.repository.AsyncPaymentTaskEntityRepository;
import dev.haritonenko.tasks.domain.async.status.AsyncPaymentTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderJpaRepository orderRepository;
    private final TransactionTemplate txTemplate;
    private final AsyncPaymentTaskEntityRepository taskRepository;

    public OrderEntity createOrder(
            OrderCreateRequestDto requestDto
    ) {
        log.info("Creation order with address {}, clientSumEstimate {}",
                requestDto.address(),
                requestDto.clientEstimate()
        );

        var orderEntity = OrderEntity.builder()
                .address(requestDto.address())
                .clientEstimate(requestDto.clientEstimate())
                .paymentStatus(PaymentStatus.NEW)
                .build();

        return txTemplate.execute(status -> {
            var createdOrder = orderRepository.save(orderEntity);
            log.info("Created order id ={}", createdOrder.getId());

            var task = AsyncPaymentTaskEntity.builder()
                    .orderId(createdOrder.getId())
                    .status(AsyncPaymentTaskStatus.NEW)
                    .processingStep(ProcessingStep.VALIDATE)
                    .attempts(0)
                    .build();

            var savedTask = taskRepository.save(task);
            log.info("Created task for order creation: taskId={}, orderId={}",
                    savedTask.getId(),
                    createdOrder.getId()
            );

            return createdOrder;
        });
    }

    public Optional<OrderEntity> findOrder(UUID id) {
        return orderRepository.findById(id);
    }
}
