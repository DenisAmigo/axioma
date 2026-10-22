package ru.amigo.axioma.insurance.dto;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:16
 */
public record QuoteRequest(
        String region,          // Регион водителя
        int age,                // Возраст водителя
        int experienceYears,    // Стаж вождения
        int horsePower,         // Мощность двигателя
        boolean isBlacklisted   // Черная метка
) {}
