package dev.haritonenko.tasks.domain.async.db.entity;

import dev.haritonenko.tasks.domain.async.status.ProcessingStep;
import dev.haritonenko.tasks.domain.async.converter.ProcessingStepConverter;
import dev.haritonenko.tasks.domain.async.status.AsyncPaymentTaskStatus;
import dev.haritonenko.tasks.domain.async.converter.AsyncPaymentStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tasks")
public class AsyncPaymentTaskEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "task_status", nullable = false)
    @Convert(converter = AsyncPaymentStatusConverter.class)
    private AsyncPaymentTaskStatus status;

    @Column(name = "processing_step", nullable = false)
    @Convert(converter = ProcessingStepConverter.class)
    private ProcessingStep processingStep;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        var now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

}