package dev.haritonenko.tasks.domain.async.db.repository;

import dev.haritonenko.tasks.domain.async.db.entity.AsyncPaymentTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AsyncPaymentTaskEntityRepository extends JpaRepository<AsyncPaymentTaskEntity, Long> {

    @Query(value = """
            SELECT * FROM tasks as t 
            where t.task_status = :newStatus
            or (t.task_status = :retryStatus and t.next_attempt_at <= :now)
            or (t.task_status = :processingStatus and t.next_attempt_at <= :now)
            order by t.id
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<AsyncPaymentTaskEntity> pickBatchForProcessing(
            @Param("newStatus") int newStatus,
            @Param("retryStatus") int retryStatus,
            @Param("processingStatus") int processingStatus,
            @Param("batchSize") int batchSize,
            @Param("now") OffsetDateTime now

    );
}
