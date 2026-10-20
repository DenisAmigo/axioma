package ru.amigo.axioma.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 1:56
 */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    List<IdempotencyKey> findByExpiresAtBefore(Instant threshold);

    void deleteByExpiresAtBefore(Instant threshold);
}
