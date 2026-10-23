package ru.amigo.axioma.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 1:00
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    // Самые старые неотправленные события
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
