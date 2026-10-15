package com.smartfit;

import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;

@SpringBootApplication
public class SmartfitApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartfitApplication.class, args);
        openBrowser("http://localhost:8080/home");
    }

    /**
     * Initializeaza baza de date la fiecare pornire:
     * - Creeaza adminul daca nu exista
     * - Creeaza / repara userul de test (reseteaza hash-ul invalid din HeidiSQL)
     *
     * CREDENTIALE:
     *   admin@smartfit.ro / admin123
     *   user@smartfit.ro  / user123
     */
    @Bean
    CommandLineRunner initDatabase(UserRepository repository, PasswordEncoder encoder) {
        return args -> {

            // ─── 1. ADMIN ─────────────────────────────────────────────────
            User admin = repository.findByEmail("admin@smartfit.ro");
            if (admin == null) {
                admin = new User();
                admin.setEmail("admin@smartfit.ro");
                admin.setFirstName("Paul");
                admin.setLastName("Admin");
                // IMPORTANT: valoare valida in ENUM('USER','EMPLOYEE','ADMIN')
                admin.setRole("ADMIN");
                admin.setFitPoints(1200);
                admin.setGoal("GAIN_MUSCLE");
                admin.setGender("M");
                admin.setHeightCm(178.0);
                admin.setWeightKg(80.0);
                admin.setBirthDate(LocalDate.of(1990, 1, 1));
                admin.setActivityLevel(1.375);
            }
            // Seteaza / reseteaza parola la fiecare pornire (util in dev)
            admin.setPasswordHash(encoder.encode("admin123"));
            repository.save(admin);
            System.out.println("✓ Admin: admin@smartfit.ro / admin123");

            // ─── 2. USER DE TEST ──────────────────────────────────────────
            // Repara si hash-ul placeholder '$2a$10$placeholder' din HeidiSQL
            User testUser = repository.findByEmail("user@smartfit.ro");
            if (testUser == null) {
                testUser = new User();
                testUser.setEmail("user@smartfit.ro");
                testUser.setFirstName("Alexandru");
                testUser.setLastName("Ionescu");
                testUser.setRole("USER");
                testUser.setFitPoints(1240);
                testUser.setGoal("GAIN_MUSCLE");
                testUser.setGender("M");
                testUser.setBirthDate(LocalDate.of(2002, 11, 14));
                testUser.setHeightCm(178.0);
                testUser.setWeightKg(78.6);
                testUser.setActivityLevel(1.375);
            }
            // Reseteaza parola indiferent (ca sa fie sigur BCrypt valid)
            testUser.setPasswordHash(encoder.encode("user123"));
            repository.save(testUser);
            System.out.println("✓ User: user@smartfit.ro / user123");
        };
    }

    private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                System.err.println("Eroare browser Desktop: " + e.getMessage());
            }
        } else {
            try {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } catch (Exception e) {
                System.err.println("Eroare ProcessBuilder: " + e.getMessage());
            }
        }
    }
}