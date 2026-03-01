package dev.haritonenko.orders.domain.db.repository;

import dev.haritonenko.orders.domain.db.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
}
