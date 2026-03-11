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
import dev.haritonenko.orders.domain.exception.IllegalStateOfAuthorizedAmountException;
import dev.haritonenko.orders.domain.exception.IllegalStateOfFinalAmountException;
import dev.haritonenko.orders.domain.service.OrderService;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncPaymentTaskProcessor {

    private final OrderJpaRepository orderRepository;
    private final OrderService orderService;
    private final WarehouseHttpClient warehouseHttpClient;
    private final PaymentStubHttpClient paymentStubHttpClient;
    private final ExecutorService externalHttpThreadPool;
    private final TransactionTemplate transactionTemplate;

    public TaskExecutionStatus processTask(AsyncPaymentTaskEntity task) {

        logCurrentProcessingStep(task);

        UUID orderId = task.getOrderId();
        Long taskId = task.getId();

        ProcessingStep stepAtStart = task.getProcessingStep();

        boolean orderExists = orderService.existsOrderById(orderId);

        if (!orderExists) {
            log.warn("Order not found: id={}", orderId);
            return TaskExecutionStatus.NON_RETRYABLE_ERROR;
        }

        if (stepAtStart == ProcessingStep.VALIDATE) {
            transactionTemplate.execute(status -> {
                task.setProcessingStep(ProcessingStep.AUTH);
                return null;
            });
            stepAtStart = ProcessingStep.AUTH;
            logCurrentProcessingStep(task);
        }

        if (stepAtStart == ProcessingStep.AUTH) {

            logCurrentProcessingStep(task);

            var clientEstimate = getClientEstimate(orderId);


            if (isNull(clientEstimate)) {
                log.warn("Client estimate is null before authorization: orderId={}, taskId={}", orderId, taskId);
                return TaskExecutionStatus.NON_RETRYABLE_ERROR;
            }
            var authorizationPayment = paymentStubHttpClient.authorizePayment(
                    AuthorizePaymentRequestDto.builder()
                            .customerId(taskId)
                            .amount(clientEstimate)
                            .build()
            );


            if (authorizationPayment.status() == AuthorizationStatus.DECLINED) {
                log.warn("Authorization payment for order with id={} declined", orderId);
                transactionTemplate.execute(status -> {
                    var order = findOrderById(orderId);
                    order.setPaymentStatus(PaymentStatus.AUTHORIZATION_FAILED);
                    order.setCancellationReason(authorizationPayment.message());
                    orderRepository.save(order);

                    task.setStatus(AsyncPaymentTaskStatus.FAILED_NON_RETRYABLE);
                    return null;
                });
                return TaskExecutionStatus.NON_RETRYABLE_ERROR;
            }

            log.info("Payment was authorized successfully for order with id={}", orderId);
            transactionTemplate.execute(status -> {
                var order = findOrderById(orderId);
                order.setAuthorizedAmount(authorizationPayment.authorizedAmount());
                order.setPaymentStatus(PaymentStatus.AUTHORIZED_SUCCESSFULLY);
                orderRepository.save(order);

                task.setProcessingStep(ProcessingStep.REPRICE);
                return null;
            });
            logCurrentProcessingStep(task);

            return TaskExecutionStatus.SUCCESS;
        }

        try {
            if (stepAtStart == ProcessingStep.REPRICE) {
                CompletableFuture<CalculatePricingResponseDto> repricingFuture = CompletableFuture
                        .supplyAsync(() -> warehouseHttpClient.calculatePricing(
                                CalculatePricingRequestDto.builder()
                                        .orderId(orderId)
                                        .build()
                        ), externalHttpThreadPool)
                        .thenApplyAsync(warehouseRepricingResult ->
                                transactionTemplate.execute(status -> {

                                    var order = findOrderById(orderId);
                                    BigDecimal authorizedAmount = order.getAuthorizedAmount();
                                    BigDecimal finalAmount = warehouseRepricingResult.finalAmount();

                                    if (authorizedAmount == null) {
                                        log.warn("Authorized amount is null before repricing compare: orderId={}", orderId);
                                        throw new IllegalStateOfAuthorizedAmountException(
                                                "Authorized amount is null before repricing (orderId=%s, taskId=%s)"
                                                        .formatted(orderId, taskId)
                                        );
                                    }
                                    if (finalAmount == null) {
                                        log.warn("Final amount is null in repricing response: orderId={}", orderId);
                                        throw new IllegalStateOfFinalAmountException(
                                                "Final amount is null in repricing response (orderId=%s, taskId=%s)"
                                                        .formatted(orderId, taskId)
                                        );
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

                                        throw new AuthorizedMoneyAmountLessThanFinalException("Final sum more than authorized");
                                    }

                                    log.info("Repricing was completed successfully for order with id={}", orderId);

                                    order.setFinalAmount(finalAmount);
                                    orderRepository.save(order);

                                    task.setProcessingStep(ProcessingStep.CAPTURE);

                                    return warehouseRepricingResult;
                                }), externalHttpThreadPool)
                        .handleAsync((result, ex) -> {
                            if (ex == null) {
                                return result;
                            }

                            Throwable exception = unwrap(ex);
                            log.warn("Handling exception={} in repricing stage", exception.getClass().getSimpleName());

                            throw new CompletionException(exception);
                        }, externalHttpThreadPool);

                repricingFuture
                        .thenCompose(warehouseRepricingResult -> {
                            var order = findOrderById(orderId);

                            return getCapturePaymentFuture(order, task);
                        })
                        .get();
                task.setStatus(AsyncPaymentTaskStatus.SUCCEEDED);

                return TaskExecutionStatus.SUCCESS;
            }

            if (stepAtStart == ProcessingStep.CAPTURE) {
                var order = findOrderById(orderId);

                getCapturePaymentFuture(order, task).get();
                task.setStatus(AsyncPaymentTaskStatus.SUCCEEDED);
                return TaskExecutionStatus.SUCCESS;
            }

            log.warn("Unsupported processing step: orderId={}, taskId={}, step={}",
                    orderId, taskId, stepAtStart);
            return TaskExecutionStatus.RETRYABLE_ERROR;

        } catch (InterruptedException ex) {
            log.warn("Thread={} was interrupted by exception ex={}",
                    Thread.currentThread().getName(),
                    ex.getClass().getSimpleName()
            );
            Thread.currentThread().interrupt();
            return TaskExecutionStatus.RETRYABLE_ERROR;

        } catch (ExecutionException ex) {
            Throwable exception = unwrap(ex);

            log.warn("Exception while creating order: orderId={},taskId={}",
                    orderId,
                    taskId,
                    exception
            );

            if (exception instanceof AuthorizedMoneyAmountLessThanFinalException
                    || exception instanceof CapturingFailedException) {
                return TaskExecutionStatus.NON_RETRYABLE_ERROR;
            }

            if (exception instanceof IllegalStateOfAuthorizedAmountException
                    || exception instanceof IllegalStateOfFinalAmountException
                    || exception instanceof IllegalStateException) {
                return TaskExecutionStatus.RETRYABLE_ERROR;
            }

            return TaskExecutionStatus.RETRYABLE_ERROR;
        }
    }

    private CompletableFuture<Void> getCapturePaymentFuture(
            OrderEntity orderForCaptureStep,
            AsyncPaymentTaskEntity task
    ) {
        UUID orderId = orderForCaptureStep.getId();
        Long taskId = task.getId();

        return CompletableFuture
                .supplyAsync(() -> {
                    BigDecimal finalAmount = transactionTemplate.execute(status -> {
                        var order = findOrderById(orderId);
                        return order.getFinalAmount();
                    });

                    if (finalAmount == null) {
                        log.warn("Error while checking final amount money (orderId={}, taskId={})",
                                orderId, taskId);
                        throw new IllegalStateOfFinalAmountException(
                                "Final amount is null before capture (orderId=%s, taskId=%s)"
                                        .formatted(orderId, taskId)
                        );
                    }

                    return paymentStubHttpClient.capturePayment(
                            CapturePaymentRequestDto.builder()
                                    .captureAmount(finalAmount)
                                    .customerId(taskId)
                                    .build()
                    );
                }, externalHttpThreadPool)
                .thenApplyAsync(capturingPaymentResult ->
                        transactionTemplate.execute(status -> {

                            if (capturingPaymentResult.status() == CaptureStatus.FAILED) {
                                log.warn("Capturing was rejected for order with id={}", orderId);

                                var order = findOrderById(orderId);
                                order.setPaymentStatus(PaymentStatus.CAPTURE_FAILED);
                                order.setCancellationReason("Capture failed by stub");
                                orderRepository.save(order);

                                task.setStatus(AsyncPaymentTaskStatus.FAILED_NON_RETRYABLE);

                                throw new CapturingFailedException("Capture failed by stub");
                            }

                            log.info("Capturing was completed successfully for order with id={}", orderId);

                            var order = findOrderById(orderId);
                            order.setCapturedAmount(capturingPaymentResult.capturedAmount());
                            order.setPaymentStatus(PaymentStatus.SUCCEED_PAID);
                            orderRepository.save(order);

                            log.info("Order with orderId={} successfully paid", orderId);

                            return null;
                        }), externalHttpThreadPool)
                .handleAsync((res, ex) -> {
                    if (ex == null) {
                        return null;
                    }

                    Throwable exception = unwrap(ex);
                    log.warn("Handling exception={} in capturing stage", exception.getClass().getSimpleName());

                    throw new CompletionException(exception);
                }, externalHttpThreadPool);
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
                || current instanceof ExecutionException
                || current instanceof CompletionException)) {
            if (current.getCause() == current) break;
            current = current.getCause();
        }
        return current;
    }

    private OrderEntity findOrderById(UUID orderId) {
        return orderService.findOrder(orderId).orElseThrow(
                () -> new IllegalStateException(
                        "Order not found during async payment processing (orderId=%s)"
                                .formatted(orderId)
                )
        );
    }
}