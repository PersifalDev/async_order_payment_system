package dev.haritonenko.tasks.domain.async.poller;

import dev.haritonenko.tasks.domain.async.db.entity.AsyncPaymentTaskEntity;
import dev.haritonenko.tasks.domain.async.db.repository.AsyncPaymentTaskEntityRepository;
import dev.haritonenko.tasks.domain.async.dispatcher.AsyncPaymentTaskDispatcher;
import dev.haritonenko.tasks.domain.async.poller.config.TaskPollerProperties;
import dev.haritonenko.tasks.domain.async.status.AsyncPaymentTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class AsyncTaskPoller {

    private final AsyncPaymentTaskEntityRepository taskRepository;
    private final TransactionTemplate transactionTemplate;
    private final AsyncPaymentTaskDispatcher taskDispatcher;
    private final TaskPollerProperties properties;

    @Scheduled(fixedDelayString = "${task-execution.poller.poll-interval-ms}")
    public void poll() {
        log.info("Starting polling task");
        List<AsyncPaymentTaskEntity> tasksBatch = pickTasksForProcessing();

        var taskIds = tasksBatch.stream()
                .filter(Objects::nonNull)
                .map(AsyncPaymentTaskEntity::getId)
                .peek(taskId -> log.info("Task with id={} was picked", taskId))
                .toList();

        if (tasksBatch.isEmpty()) {
            log.warn("No tasks for polling");
            return;
        }
        log.info("Successfully picked tasks: count={}",
                taskIds.size());

        for (var task : tasksBatch) {
            taskDispatcher.dispatchTask(task);
        }
    }

    private List<AsyncPaymentTaskEntity> pickTasksForProcessing() {

        log.info("Picking tasks");

        return transactionTemplate.execute(status -> {
            List<AsyncPaymentTaskEntity> tasks = taskRepository.pickBatchForProcessing(
                    AsyncPaymentTaskStatus.NEW.getCode(),
                    AsyncPaymentTaskStatus.FAILED_RETRYABLE.getCode(),
                    AsyncPaymentTaskStatus.IN_PROGRESS.getCode(),
                    properties.getBatchSize(),
                    OffsetDateTime.now()
            );
            var nextProcessTime = OffsetDateTime.now().plus(properties.getRetryDelay());
            for (var task : tasks) {
                task.setStatus(AsyncPaymentTaskStatus.IN_PROGRESS);

                var attempts = task.getAttempts() == null ? 1 : task.getAttempts() + 1;
                task.setAttempts(attempts);
                task.setNextAttemptAt(nextProcessTime);
                log.info("Task with id={} was picked and updated with new attempts value={}",
                        task.getId(),
                        task.getAttempts()
                );
            }
            taskRepository.saveAll(tasks);
            return tasks;
        });
    }

}
