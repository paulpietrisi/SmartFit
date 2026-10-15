package com.smartfit.service;

import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.Period;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    /**
     * Salveaza fotografia de profil a utilizatorului si sterge versiunea veche daca exista.
     */
    public String saveProfilePhoto(String email, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fișierul este gol.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg")
                && !contentType.equals("image/png")
                && !contentType.equals("image/webp"))) {
            throw new RuntimeException("Format invalid. Acceptat: JPG, PNG, WebP.");
        }

        if (file.getSize() > 5L * 1024 * 1024) {
            throw new RuntimeException("Fișierul depășește 5MB.");
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Utilizatorul cu email-ul " + email + " nu există.");
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";

        String fileName = "user_" + user.getId() + "_" + System.currentTimeMillis() + ext;

        Path uploadPath = Paths.get(uploadDir, "profile-photos");
        Files.createDirectories(uploadPath);
        Path dest = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        String photoUrl = "/uploads/profile-photos/" + fileName;

        if (user.getProfilePhoto() != null && !user.getProfilePhoto().isBlank()) {
            try {
                String oldRelative = user.getProfilePhoto().replaceFirst("^/uploads/", "");
                Path oldFile = Paths.get(uploadDir).resolve(oldRelative);
                Files.deleteIfExists(oldFile);
            } catch (Exception ignored) {}
        }

        user.setProfilePhoto(photoUrl);
        userRepository.save(user);

        return photoUrl;
    }

    /**
     * Sterge fizic poza de profil de pe disc si reseteaza campul la null in DB.
     */
    public void deleteProfilePhoto(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Utilizatorul nu există.");
        }

        if (user.getProfilePhoto() != null && !user.getProfilePhoto().isBlank()) {
            try {
                String oldRelative = user.getProfilePhoto().replaceFirst("^/uploads/", "");
                Path oldFile = Paths.get(uploadDir).resolve(oldRelative);
                Files.deleteIfExists(oldFile);
            } catch (Exception ignored) {}
        }

        user.setProfilePhoto(null);
        userRepository.save(user);
    }

    /**
     * Calculeaza BMR (Basal Metabolic Rate) folosind formula Mifflin-St Jeor.
     */
    public double calculateBMR(User user) {
        if (user.getWeightKg() == null || user.getHeightCm() == null || user.getBirthDate() == null) {
            return 1800;
        }

        int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        double sexOffset = "F".equalsIgnoreCase(user.getGender()) ? -161 : 5;

        return (10 * user.getWeightKg())
                + (6.25 * user.getHeightCm())
                - (5.0 * age)
                + sexOffset;
    }

    /**
     * Calculeaza tinta calorica zilnica (TDEE) pe baza BMR si nivelului de activitate.
     */
    public double calculateDailyTarget(User user) {
        double bmr = calculateBMR(user);
        double activityFactor = (user.getActivityLevel() != null) ? user.getActivityLevel() : 1.375;
        double tdee = bmr * activityFactor;

        if ("LOSE_WEIGHT".equals(user.getGoal())) return tdee - 500;
        if ("GAIN_MUSCLE".equals(user.getGoal()))  return tdee + 300;
        return tdee;
    }
}