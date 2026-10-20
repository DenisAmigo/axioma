package ru.amigo.axioma.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.amigo.axioma.account.Account;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 1:54
 */
@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Transfer> findByFromAccount(Account fromAccount);

    List<Transfer> findByToAccount(Account toAccount);

    Page<Transfer> findByFromAccountOrderByCreatedAtDesc(Account fromAccount, Pageable pageable);

    Page<Transfer> findByToAccountOrderByCreatedAtDesc(Account toAccount, Pageable pageable);

    List<Transfer> findByStatus(TransferStatus status);

    List<Transfer> findByStatusAndCreatedAtBefore(TransferStatus status, Instant threshold);

    @Query("SELECT t FROM Transfer t WHERE t.fromAccount.user.id = :userId OR t.toAccount.user.id = :userId")
    Page<Transfer> findByFromAccountUserId(@Param("userId") Long userId, Pageable pageable);
}
