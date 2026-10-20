package ru.amigo.axioma.deposit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 21:41
 */
@Repository
public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {
    Optional<DepositTransaction> findByIdempotencyKey(String idempotencyKey);
}
