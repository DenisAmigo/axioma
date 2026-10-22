package ru.amigo.axioma.insurance.domain;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:14
 */
@Component
public class CompanyRegistry {
    public List<InsuranceCompany> getCompanies() {
        return List.of(
                new InsuranceCompany("ALPHA", "АльфаСтрахование"),
                new InsuranceCompany("BETA", "ВСК"),
                new InsuranceCompany("GAMMA", "Гамма-Страх")
        );
    }
}
