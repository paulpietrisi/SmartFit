package com.smartfit.util;

import com.smartfit.entity.User;
import java.time.LocalDate;
import java.time.Period;

/**
 * Utilitar pentru calculul BMR (Mifflin‑St Jeor) și TDEE.
 */
public class BmrCalculator {

    private BmrCalculator() {} // constructor privat – doar metode statice

    /**
     * Calculează BMR folosind formula Mifflin‑St Jeor.
     * @param user utilizatorul (trebuie să aibă greutate, înălțime, data nașterii și gen)
     * @return BMR în kcal/zi
     * @throws IllegalArgumentException dacă user este null sau lipsesc date esențiale
     */
    public static double calculateBmr(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Utilizatorul nu poate fi null");
        }
        Double weight = user.getWeightKg();
        Double height = user.getHeightCm();
        LocalDate birth = user.getBirthDate();
        String gender = user.getGender();

        if (weight == null || height == null || birth == null || gender == null) {
            throw new IllegalArgumentException("Date incomplete: greutate, înălțime, data nașterii și genul sunt obligatorii");
        }

        int age = Period.between(birth, LocalDate.now()).getYears();
        if (age < 0) age = 0; // siguranță

        double bmr;
        if ("F".equalsIgnoreCase(gender)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        }
        return Math.round(bmr * 100.0) / 100.0; // rotunjire la 2 zecimale
    }

    /**
     * Calculează TDEE = BMR * factor activitate.
     * @param user utilizatorul (trebuie să aibă și activityLevel)
     * @return TDEE în kcal/zi
     * @throws IllegalArgumentException dacă user este null sau lipsește activityLevel
     */
    public static double calculateTdee(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Utilizatorul nu poate fi null");
        }
        Double activityLevel = user.getActivityLevel();
        if (activityLevel == null) {
            throw new IllegalArgumentException("Lipsește nivelul de activitate");
        }
        double bmr = calculateBmr(user);
        return Math.round(bmr * activityLevel * 100.0) / 100.0;
    }
}