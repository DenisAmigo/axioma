package ru.amigo.axioma.insurance.domain;

import org.springframework.stereotype.Component;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:34
 */
@Component
public class InsuranceCoefficients {

    // КТ: Территориальный коэффициент
    public double getRegionCoef(String region) {
        return switch (region.toUpperCase()) {
            case "MSK" -> 1.8;
            case "SPB" -> 1.6;
            case "BEL" -> 1.0;
            default -> 1.2;
        };
    }

    // КВС: Возраст и стаж (матрица)
    public double getAgeExperienceCoef(int age, int experienceYears) {
        if (age < 22) {
            return experienceYears <= 3 ? 1.8 : 1.6;
        } else if (age <= 30) {
            return experienceYears <= 3 ? 1.5 : 1.2;
        } else {
            return experienceYears > 10 ? 0.8 : 1.0;
        }
    }

    // КМ: Мощность двигателя (л.с.)
    public double getPowerCoef(int horsePower) {
        if (horsePower <= 50) return 0.6;
        if (horsePower <= 100) return 1.0;
        if (horsePower <= 150) return 1.4;
        return 1.6;
    }

    // Базовые ставки компаний (у кого-то дешевле для молодых, у кого-то для опытных)
    public double getBaseRate(String companyCode) {
        return switch (companyCode.toUpperCase()) {
            case "ALPHA" -> 5000.0;
            case "BETA" -> 4800.0;
            case "GAMMA" -> 4500.0;
            default -> 5000.0;
        };
    }
}
