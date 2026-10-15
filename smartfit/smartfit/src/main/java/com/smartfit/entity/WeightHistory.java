package com.smartfit.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "weight_history")
public class WeightHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "logged_date", nullable = false)
    private LocalDate loggedDate;

    public Long getId()              { return id; }
    public User getUser()            { return user; }
    public Double getWeightKg()      { return weightKg; }
    public LocalDate getLoggedDate() { return loggedDate; }

    public void setUser(User user)            { this.user = user; }
    public void setWeightKg(Double w)         { this.weightKg = w; }
    public void setLoggedDate(LocalDate d)    { this.loggedDate = d; }
}