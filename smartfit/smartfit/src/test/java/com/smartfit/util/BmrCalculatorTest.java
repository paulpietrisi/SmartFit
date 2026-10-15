package com.smartfit.util;

import com.smartfit.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BmrCalculator - Teste formula Mifflin-St Jeor")
class BmrCalculatorTest {

    private User userMasculin;
    private User userFeminin;

    @BeforeEach
    void setUp() {
        userMasculin = new User();
        userMasculin.setWeightKg(78.6);
        userMasculin.setHeightCm(178.0);
        userMasculin.setBirthDate(LocalDate.now().minusYears(23));
        userMasculin.setGender("M");
        userMasculin.setActivityLevel(1.375);

        userFeminin = new User();
        userFeminin.setWeightKg(60.0);
        userFeminin.setHeightCm(165.0);
        userFeminin.setBirthDate(LocalDate.now().minusYears(28));
        userFeminin.setGender("F");
        userFeminin.setActivityLevel(1.2);
    }

    @Test
    @DisplayName("BMR masculin: 78.6 kg, 178 cm, 23 ani → 1788.5")
    void calcBmr_masculin() {
        double bmr = BmrCalculator.calculateBmr(userMasculin);
        assertEquals(1788.5, bmr, 0.01);
    }

    @Test
    @DisplayName("BMR feminin: 60 kg, 165 cm, 28 ani → 1330.25")
    void calcBmr_feminin() {
        double bmr = BmrCalculator.calculateBmr(userFeminin);
        assertEquals(1330.25, bmr, 0.01);
    }

    @Test
    @DisplayName("BMR este pozitiv pentru date reale")
    void calcBmr_pozitiv() {
        assertTrue(BmrCalculator.calculateBmr(userMasculin) > 0);
    }

    @Test
    @DisplayName("BMR crește cu greutatea")
    void calcBmr_greutateMaiMare() {
        User userGreu = new User();
        userGreu.setWeightKg(100.0);
        userGreu.setHeightCm(178.0);
        userGreu.setBirthDate(LocalDate.now().minusYears(23));
        userGreu.setGender("M");

        double bmrNormal = BmrCalculator.calculateBmr(userMasculin);
        double bmrGreu = BmrCalculator.calculateBmr(userGreu);
        assertTrue(bmrGreu > bmrNormal);
    }

    @Test
    @DisplayName("TDEE masculin = BMR * 1.375")
    void calcTdee_masculin() {
        double bmr = BmrCalculator.calculateBmr(userMasculin);
        double tdee = BmrCalculator.calculateTdee(userMasculin);
        assertEquals(bmr * 1.375, tdee, 0.01);
    }

    @Test
    @DisplayName("TDEE feminin = BMR * 1.2")
    void calcTdee_feminin() {
        double bmr = BmrCalculator.calculateBmr(userFeminin);
        double tdee = BmrCalculator.calculateTdee(userFeminin);
        assertEquals(bmr * 1.2, tdee, 0.01);
    }

    @ParameterizedTest(name = "{0} kg, {1} cm, {2} ani, gen={3} → {4}")
    @CsvSource({
            "70.0, 175.0, 25, M, 1673.75",
            "55.0, 160.0, 30, F, 1239.0",
            "90.0, 180.0, 40, M, 1830.0",
            "45.0, 155.0, 20, F, 1157.75"
    })
    @DisplayName("BMR parametrizat (valori reale)")
    void calcBmr_parametrizat(double kg, double cm, int varsta,
                              String gen, double bmrAsteptat) {
        User u = new User();
        u.setWeightKg(kg);
        u.setHeightCm(cm);
        u.setBirthDate(LocalDate.now().minusYears(varsta));
        u.setGender(gen);

        double bmr = BmrCalculator.calculateBmr(u);
        assertEquals(bmrAsteptat, bmr, 0.01);
    }

    @Test
    @DisplayName("BMR cu greutate minimă (30 kg) nu aruncă excepție")
    void calcBmr_greutateMinima() {
        userMasculin.setWeightKg(30.0);
        assertDoesNotThrow(() -> BmrCalculator.calculateBmr(userMasculin));
    }

    @Test
    @DisplayName("calculateBmr aruncă excepție dacă user null")
    void calcBmr_userNull() {
        assertThrows(IllegalArgumentException.class,
                () -> BmrCalculator.calculateBmr(null));
    }
}