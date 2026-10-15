package com.smartfit.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "log_date"}))
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "water_ml")
    private Integer waterMl = 0;

    @Column(name = "steps")
    private Integer steps = 0;

    @Column(name = "sleep_minutes")
    private Integer sleepMinutes = 0;

    public Long getId()                  { return id; }
    public void setId(Long id)           { this.id = id; }

    public User getUser()                { return user; }
    public void setUser(User user)       { this.user = user; }

    public LocalDate getLogDate()        { return logDate; }
    public void setLogDate(LocalDate d)  { this.logDate = d; }

    public Integer getWaterMl()          { return waterMl; }
    public void setWaterMl(Integer v)    { this.waterMl = v; }

    public Integer getSteps()            { return steps; }
    public void setSteps(Integer v)      { this.steps = v; }

    public Integer getSleepMinutes()     { return sleepMinutes; }
    public void setSleepMinutes(Integer v){ this.sleepMinutes = v; }
}