package ru.amigo.axioma.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.transfer.Transfer;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 1:55
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransfer(Transfer transfer);

    List<LedgerEntry> findByAccount(Account account);

    Page<LedgerEntry> findByAccountOrderByCreatedAtDesc(Account account, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE (0 - le.amount) END), 0) " +
            "FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal calculateBalance(@Param("accountId") Long accountId);

    @Query("SELECT le FROM LedgerEntry le WHERE le.transfer.id = :transferId")
    List<LedgerEntry> findAllByTransferId(@Param("transferId") Long transferId);
}
