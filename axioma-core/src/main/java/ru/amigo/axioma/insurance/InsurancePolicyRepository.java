package ru.amigo.axioma.insurance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 19:58
 */
@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {
    List<InsurancePolicy> findByStatusAndExpiresAtBefore(PolicyStatus status, Instant time);
}
