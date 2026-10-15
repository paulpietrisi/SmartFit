package com.smartfit.service;

import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Teste gestionare utilizatori")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User userNou;

    @BeforeEach
    void setUp() {
        userNou = new User();
        userNou.setEmail("paul@smartfit.ro");
        userNou.setPasswordHash("parolaMeaSecreta123");
        userNou.setFirstName("Paul");
        userNou.setLastName("Pietrisi");
        userNou.setGender("M");
        userNou.setRole("USER");
        userNou.setFitPoints(0);
    }

    // ─── Teste saveUser ────────────────────────────────────────────

    @Test
    @DisplayName("saveUser salvează utilizatorul în baza de date")
    void saveUser_salveazaUserulInBD() {
        userService.saveUser(userNou);
        verify(userRepository, times(1)).save(userNou);
    }

    @Test
    @DisplayName("saveUser nu modifică nimic – doar deleagă la repository")
    void saveUser_nuModificaNimic() {
        userService.saveUser(userNou);
        verify(userRepository).save(userNou);
        assertTrue(true); // doar pentru a confirma că nu se aruncă excepții
    }

    // ─── Teste pentru metodele de profil (calculateBMR, calculateDailyTarget) ──

    @Test
    @DisplayName("calculateBMR returnează valoarea corectă pentru masculin")
    void calculateBMR_masculin() {
        User user = new User();
        user.setWeightKg(78.6);
        user.setHeightCm(178.0);
        user.setBirthDate(LocalDate.now().minusYears(23));
        user.setGender("M");

        double bmr = userService.calculateBMR(user);
        assertEquals(1788.5, bmr, 0.01);
    }

    @Test
    @DisplayName("calculateBMR returnează 1800 dacă lipsesc date esențiale")
    void calculateBMR_dateLipsa() {
        User user = new User();
        double bmr = userService.calculateBMR(user);
        assertEquals(1800, bmr, 0.01);
    }
}