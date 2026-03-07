package dev.haritonenko.orders.domain.db.entity;

import dev.haritonenko.orders.domain.status.PaymentStatus;
import dev.haritonenko.orders.domain.converter.PaymentStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @UuidGenerator
    private UUID id;

    @Column(name = "address")
    private String address;

    @Column(name = "client_estimate", precision = 12, scale = 2)
    private BigDecimal clientEstimate;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "authorized_amount", precision = 12, scale = 2)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", precision = 12, scale = 2)
    private BigDecimal capturedAmount;

    @Column(name = "payment_status", nullable = false)
    @Convert(converter = PaymentStatusConverter.class)
    private PaymentStatus paymentStatus;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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
