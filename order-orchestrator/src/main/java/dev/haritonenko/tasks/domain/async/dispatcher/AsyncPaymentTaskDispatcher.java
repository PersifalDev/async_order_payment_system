package dev.haritonenko.tasks.domain.async.dispatcher;

import dev.haritonenko.tasks.domain.async.dispatcher.config.TaskDispatcherProperties;
import dev.haritonenko.tasks.domain.async.db.entity.AsyncPaymentTaskEntity;
import dev.haritonenko.tasks.domain.async.db.repository.AsyncPaymentTaskEntityRepository;
import dev.haritonenko.tasks.domain.async.processor.AsyncPaymentTaskProcessor;
import dev.haritonenko.tasks.domain.async.status.AsyncPaymentTaskStatus;
import dev.haritonenko.tasks.domain.async.status.TaskExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncPaymentTaskDispatcher {

    private final ExecutorService taskDispatcherThreadPool;
    private final TransactionTemplate transactionTemplate;
    private final AsyncPaymentTaskEntityRepository taskRepository;
    private final AsyncPaymentTaskProcessor taskProcessor;
    private final TaskDispatcherProperties properties;

    public void dispatchTask(AsyncPaymentTaskEntity task) {

        CompletableFuture
                .supplyAsync(() -> taskProcessor.processTask(task), taskDispatcherThreadPool)
                .thenAccept(result -> handleTaskExecuted(task, result))
                .exceptionally(ex -> handleExceptionInTaskHappened(task, ex));
    }

    private Void handleExceptionInTaskHappened(
            AsyncPaymentTaskEntity task,
            Throwable ex
    ) {
        log.warn("Task failed with unexpected exception: taskId={}", task.getId(), ex);
        scheduleTaskRetry(task);
        log.warn("Success processed task failure:taskId={}", task.getId(), ex);
        return null;
    }

    private void handleTaskExecuted(
            AsyncPaymentTaskEntity task,
            TaskExecutionStatus taskExecutionStatus
    ) {
        log.info("Task successfully executed: taskId={},taskExecutionStatus={}",
                task.getId(), taskExecutionStatus);

        switch (taskExecutionStatus) {
            case SUCCESS -> handleTaskSucceeded(task);
            case RETRYABLE_ERROR -> scheduleTaskRetry(task);
            case NON_RETRYABLE_ERROR -> handleTaskFailed(task);
        }
        log.info("Success processed execution status: taskId={}, status={}", task.getId(), taskExecutionStatus);
    }

    private void handleTaskFailed(AsyncPaymentTaskEntity task) {

        transactionTemplate.execute(status ->
                taskRepository.save(task.toBuilder()
                        .status(AsyncPaymentTaskStatus.FAILED_NON_RETRYABLE)
                        .nextAttemptAt(null)
                        .build())
        );
    }

    private void scheduleTaskRetry(AsyncPaymentTaskEntity task) {

        log.info("Scheduling default retry for taskId={}", task.getId());

        if (task.getAttempts() != null && task.getAttempts() >= properties.getMaxAttempts()) {
            log.warn("Maximum number of retries reached: taskId={}", task.getId());
            handleTaskFailed(task);
            return;
        }

        var nextAttemptTime = OffsetDateTime.now().plus(properties.getRetryDelay());

        transactionTemplate.execute(status ->
                taskRepository.save(task.toBuilder()
                        .status(AsyncPaymentTaskStatus.FAILED_RETRYABLE)
                        .nextAttemptAt(nextAttemptTime)
                        .build())
        );
    }

    private void handleTaskSucceeded(AsyncPaymentTaskEntity task) {

        if (task.getStatus() == AsyncPaymentTaskStatus.SUCCEEDED) {
            transactionTemplate.execute(status ->
                    taskRepository.save(task.toBuilder()
                            .status(AsyncPaymentTaskStatus.SUCCEEDED)
                            .nextAttemptAt(null)
                            .build())
            );
            return;
        }

        transactionTemplate.execute(status ->
                taskRepository.save(task.toBuilder()
                        .status(AsyncPaymentTaskStatus.NEW)
                        .nextAttemptAt(OffsetDateTime.now())
                        .build())
        );
    }
}