package dev.haritonenko.tasks.domain.async.processor;

import dev.haritonenko.api.payment.authorization.AuthorizationStatus;
import dev.haritonenko.api.payment.authorization.dto.AuthorizePaymentRequestDto;
import dev.haritonenko.api.payment.capture.CaptureStatus;
import dev.haritonenko.api.payment.capture.dto.CapturePaymentRequestDto;
import dev.haritonenko.api.warehouse.dto.CalculatePricingRequestDto;
import dev.haritonenko.api.warehouse.dto.CalculatePricingResponseDto;
import dev.haritonenko.orders.domain.db.entity.OrderEntity;
import dev.haritonenko.orders.domain.db.repository.OrderJpaRepository;
import dev.haritonenko.orders.domain.exception.AuthorizedMoneyAmountLessThanFinalException;
import dev.haritonenko.orders.domain.exception.CapturingFailedException;
import dev.haritonenko.orders.domain.status.PaymentStatus;
import dev.haritonenko.orders.external.payment_stub.PaymentStubHttpClient;
import dev.haritonenko.orders.external.warehouse.WarehouseHttpClient;
import dev.haritonenko.tasks.domain.async.db.entity.AsyncPaymentTaskEntity;
import dev.haritonenko.tasks.domain.async.status.AsyncPaymentTaskStatus;
import dev.haritonenko.tasks.domain.async.status.ProcessingStep;
import dev.haritonenko.tasks.domain.async.status.TaskExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncPaymentTaskProcessor {

    private final OrderJpaRepository orderRepository;
    private final WarehouseHttpClient warehouseHttpClient;
    private final PaymentStubHttpClient paymentStubHttpClient;
    private final ExecutorService externalHttpThreadPool;
    private final TransactionTemplate transactionTemplate;

    public TaskExecutionStatus processTask(AsyncPaymentTaskEntity task) {

        logCurrentProcessingStep(task);

        UUID orderId = task.getOrderId();
        Long taskId = task.getId();

        boolean orderExists = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                orderRepository.existsById(orderId)
        ));
        if (!orderExists) {
            log.warn("Order not found: id={}", orderId);
            return TaskExecutionStatus.NON_RETRYABLE_ERROR;
        }

        transactionTemplate.execute(status -> {
            task.setProcessingStep(ProcessingStep.AUTH);
            return null;
        });
        logCurrentProcessingStep(task);

        var authorizationPayment = paymentStubHttpClient.authorizePayment(
                AuthorizePaymentRequestDto.builder()
                        .customerId(taskId)
                        .amount(getClientEstimate(orderId))
                        .build()
        );

        if (authorizationPayment.status() == AuthorizationStatus.DECLINED) {
            log.warn("Authorization payment for order with id={} declined", orderId);
            transactionTemplate.execute(status -> {
                OrderEntity order = orderRepository.findById(orderId).orElseThrow();
                order.setPaymentStatus(PaymentStatus.AUTHORIZATION_FAILED);
                order.setCancellationReason(authorizationPayment.message());
                orderRepository.save(order);

                task.setStatus(AsyncPaymentTaskStatus.FAILED_NON_RETRYABLE);
                return null;
            });
            return TaskExecutionStatus.SUCCESS;
        }

        log.info("Payment was authorized successfully for order with id={}", orderId);
        transactionTemplate.execute(status -> {
            var order = orderRepository.findById(orderId).orElseThrow();
            order.setAuthorizedAmount(authorizationPayment.authorizedAmount());
            order.setPaymentStatus(PaymentStatus.AUTHORIZED_SUCCESSFULLY);
            orderRepository.save(order);

            task.setProcessingStep(ProcessingStep.REPRICE);
            return null;
        });
        logCurrentProcessingStep(task);

        CompletableFuture<Optional<CalculatePricingResponseDto>> repricingFuture =
                CompletableFuture.supplyAsync(() -> warehouseHttpClient.calculatePricing(
                                CalculatePricingRequestDto.builder()
                                        .orderId(orderId)
                                        .build()
                        ), externalHttpThreadPool)
                        .thenApplyAsync(repricing ->
                                transactionTemplate.execute(status -> {

                                    OrderEntity order = orderRepository.findById(orderId).orElseThrow();

                                    BigDecimal authorizedAmount = order.getAuthorizedAmount();
                                    BigDecimal finalAmount = repricing.finalAmount();

                                    if (authorizedAmount == null) {
                                        log.warn("Authorized amount is null before repricing compare: orderId={}", orderId);
                                        throw new IllegalStateException("Authorized amount is null");
                                    }
                                    if (finalAmount == null) {
                                        log.warn("Final amount is null in repricing response: orderId={}", orderId);
                                        throw new IllegalStateException("Final amount is null");
                                    }

                                    if (finalAmount.compareTo(authorizedAmount) > 0) {
                                        log.warn("Repricing was rejected for order with id={}", orderId);

                                        order.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
                                        order.setCancellationReason("Price has changed from %s to %s".formatted(
                                                order.getClientEstimate(),
                                                finalAmount
                                        ));
                                        orderRepository.save(order);

                                        task.setStatus(AsyncPaymentTaskStatus.FAILED_NON_RETRYABLE);
                                        return Optional.<CalculatePricingResponseDto>empty();
                                    }

                                    log.info("Repricing was completed successfully for order with id={}", orderId);

                                    order.setFinalAmount(finalAmount);
                                    orderRepository.save(order);

                                    task.setProcessingStep(ProcessingStep.CAPTURE);
                                    return Optional.of(repricing);
                                }), externalHttpThreadPool)
                        .handleAsync((opt, ex) -> {
                            if (ex == null) {
                                return opt;
                            }

                            Throwable exception = unwrap(ex);
                            log.warn("Handling exception={} in repricing stage", exception.getClass().getSimpleName());

                            if (exception instanceof AuthorizedMoneyAmountLessThanFinalException) {
                                transactionTemplate.execute(status -> {
                                    OrderEntity order = orderRepository.findById(orderId).orElseThrow();
                                    order.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
                                    order.setCancellationReason(exception.getMessage());
                                    orderRepository.save(order);

                                    task.setStatus(AsyncPaymentTaskStatus.FAILED_NON_RETRYABLE);
                                    return null;
                                });
                                return Optional.empty();
                            }

                            if (exception instanceof IllegalStateException) {
                                return Optional.empty();
                            }

                            throw new RuntimeException(exception);
                        }, externalHttpThreadPool);

        try {
            repricingFuture
                    .thenCompose(optRepricing -> {

                        if (optRepricing.isEmpty()) {
                            return CompletableFuture.completedFuture(Optional.empty());
                        }

                        return CompletableFuture.supplyAsync(() -> {

                                    BigDecimal finalAmount = transactionTemplate.execute(status -> {
                                        OrderEntity order = orderRepository.findById(orderId).orElseThrow();
                                        return order.getFinalAmount();
                                    });

                                    if (finalAmount == null) {
                                        throw new IllegalStateException("Final amount is null before capture");
                                    }

                                    return paymentStubHttpClient.capturePayment(
                                            CapturePaymentRequestDto.builder()
                                                    .captureAmount(finalAmount)
                                                    .customerId(taskId)
                                                    .build()
                                    );
                                }, externalHttpThreadPool)
                                .thenApplyAsync(capture -> transactionTemplate.execute(status -> {

                                    if (capture.status() == CaptureStatus.FAILED) {
                                        log.warn("Capturing was rejected for order with id={}", orderId);
                                        var order = orderRepository.findById(orderId).orElseThrow();
                                        order.setPaymentStatus(PaymentStatus.CAPTURE_FAILED);
                                        order.setCancellationReason("Capture failed by stub");
                                        orderRepository.save(order);

                                        task.setStatus(AsyncPaymentTaskStatus.FAILED_NON_RETRYABLE);
                                        throw new CapturingFailedException("Capture failed by stub");
                                    }

                                    log.info("Capturing was completed successfully for order with id={}", orderId);

                                    var order = orderRepository.findById(orderId).orElseThrow();
                                    order.setCapturedAmount(capture.capturedAmount());
                                    order.setPaymentStatus(PaymentStatus.SUCCEED_PAID);
                                    orderRepository.save(order);

                                    log.info("Order with orderId={} successfully paid", orderId);

                                    return Optional.of(capture);
                                }), externalHttpThreadPool)
                                .handleAsync((res, ex) -> {
                                    if (ex == null) {
                                        return res;
                                    }

                                    Throwable exception = unwrap(ex);
                                    log.warn("Handling exception={} in capturing stage", exception.getClass().getSimpleName());

                                    if (exception instanceof CapturingFailedException) {
                                        return Optional.empty();
                                    }

                                    if (exception instanceof IllegalStateException) {
                                        return Optional.empty();
                                    }

                                    throw new RuntimeException(exception);
                                }, externalHttpThreadPool);
                    })
                    .get();

        } catch (InterruptedException ex) {
            log.warn("Thread={} was interrupted by exception ex={}",
                    Thread.currentThread().getName(),
                    ex.getClass().getSimpleName()
            );
            Thread.currentThread().interrupt();
            return TaskExecutionStatus.RETRYABLE_ERROR;
        } catch (ExecutionException ex) {
            log.warn("Exception while creating order: orderId={},taskId={}",
                    orderId,
                    taskId,
                    ex
            );
            return TaskExecutionStatus.RETRYABLE_ERROR;
        }

        return TaskExecutionStatus.SUCCESS;
    }

    private BigDecimal getClientEstimate(UUID orderId) {
        return transactionTemplate.execute(status -> orderRepository.findById(orderId)
                .map(OrderEntity::getClientEstimate)
                .orElse(null));
    }

    private void logCurrentProcessingStep(AsyncPaymentTaskEntity task) {
        log.info("Task processing step={}:", task.getProcessingStep().getValue());
    }

    private Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && (current instanceof RuntimeException
                || current instanceof ExecutionException)) {
            if (current.getCause() == current) break;
            current = current.getCause();
        }
        return current;
    }
}