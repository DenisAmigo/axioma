package ru.amigo.axioma.insurance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import ru.amigo.axioma.user.User;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 23:03
 */
@Entity
@Table(name = "insurance_quotes")
public class InsuranceQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_code", nullable = false, length = 50)
    private String companyCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "coefficient_info", length = 255)
    private String coefficientInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InsuranceQuote() {
        // Для JPA
    }

    public InsuranceQuote(User user, String companyCode, BigDecimal amount, String coefficientInfo) {
        this.user = user;
        this.companyCode = companyCode;
        this.amount = amount;
        this.coefficientInfo = coefficientInfo;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getCompanyCode() { return companyCode; }
    public BigDecimal getAmount() { return amount; }
    public String getCoefficientInfo() { return coefficientInfo; }
    public Instant getCreatedAt() { return createdAt; }
}
