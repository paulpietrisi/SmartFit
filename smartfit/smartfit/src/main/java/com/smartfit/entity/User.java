package com.smartfit.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    // Valori valide in DB: 'USER', 'EMPLOYEE', 'ADMIN'
    private String role;

    @Column(name = "fit_points")
    private Integer fitPoints = 0;

    private String gender; // 'M' sau 'F'

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    // Valori valide: 'LOSE_WEIGHT', 'MAINTAIN', 'GAIN_MUSCLE'
    private String goal;

    // Factor de activitate: 1.2 / 1.375 / 1.55 / 1.725
    // Coloana va fi creata automat de Hibernate (ddl-auto=update)
    @Column(name = "activity_level")
    private Double activityLevel = 1.375;

    @Column(name = "profile_photo")
    private String profilePhoto;

    // Constructor gol obligatoriu pentru JPA
    public User() {}

    // =========================================================
    // GETTERE SI SETTERE
    // =========================================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getFitPoints() { return fitPoints; }
    public void setFitPoints(Integer fitPoints) { this.fitPoints = fitPoints; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public Double getActivityLevel() { return activityLevel; }
    public void setActivityLevel(Double activityLevel) { this.activityLevel = activityLevel; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
}