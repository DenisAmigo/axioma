package ru.amigo.axioma.insurance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 23:07
 */
@Repository
public interface InsuranceQuoteRepository extends JpaRepository<InsuranceQuote, Long> {
    List<InsuranceQuote> findByUserId(Long userId);
}
