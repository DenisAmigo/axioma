package ru.amigo.axioma.insurance.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.amigo.axioma.insurance.domain.CompanyRegistry;
import ru.amigo.axioma.insurance.domain.InsuranceCoefficients;
import ru.amigo.axioma.insurance.domain.InsuranceCompany;
import ru.amigo.axioma.insurance.dto.QuoteRequest;
import ru.amigo.axioma.insurance.dto.QuoteResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:18
 */
@RestController
@RequestMapping("/api/insurance")
public class InsuranceController {

    private static final Logger log = LoggerFactory.getLogger(InsuranceController.class);
    private final CompanyRegistry companyRegistry;
    private final InsuranceCoefficients coefficients;
    private final Random random = new Random();

    public InsuranceController(CompanyRegistry companyRegistry, InsuranceCoefficients coefficients) {
        this.companyRegistry = companyRegistry;
        this.coefficients = coefficients;
    }

    @PostMapping("/quotes")
    public List<QuoteResponse> getQuotes(@RequestBody QuoteRequest request) {
        List<QuoteResponse> results = new ArrayList<>();

        // Получаем общие коэффициенты для клиента
        double kt = coefficients.getRegionCoef(request.region());
        double kvs = coefficients.getAgeExperienceCoef(request.age(), request.experienceYears());
        double km = coefficients.getPowerCoef(request.horsePower());

        for (InsuranceCompany company : companyRegistry.getCompanies()) {
            // Рандомайзер: с вероятностью 30% компания "не отвечает"
            if (random.nextInt(100) < 30) {
                log.warn("Компания {} временно недоступна (имитация сбоя)", company.code());
                continue;
            }

            double baseRate = coefficients.getBaseRate(company.code());
            double blacklistCoef = (request.isBlacklisted() && "ALPHA".equals(company.code())) ? 1.5 : 1.0;

            BigDecimal amount = BigDecimal.valueOf(baseRate)
                    .multiply(BigDecimal.valueOf(kt))
                    .multiply(BigDecimal.valueOf(kvs))
                    .multiply(BigDecimal.valueOf(km))
                    .multiply(BigDecimal.valueOf(blacklistCoef))
                    .setScale(2, RoundingMode.HALF_UP);

            results.add(new QuoteResponse(company.code(), company.name(), amount));
        }

        return results;
    }
}
