package com.smartfit.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "diary_entries")
public class DiaryEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Column(name = "consumed_date", nullable = false)
    private LocalDate consumedDate;

    @Column(name = "meal_type", nullable = false)
    private String mealType;

    @Column(name = "quantity_g", nullable = false)
    private Double quantityG;

    public Long getId()              { return id; }
    public User getUser()            { return user; }
    public Food getFood()            { return food; }
    public LocalDate getConsumedDate(){ return consumedDate; }
    public String getMealType()      { return mealType; }
    public Double getQuantityG()     { return quantityG; }

    public void setUser(User u)           { this.user = u; }
    public void setFood(Food f)           { this.food = f; }
    public void setConsumedDate(LocalDate d){ this.consumedDate = d; }
    public void setMealType(String m)     { this.mealType = m; }
    public void setQuantityG(Double q)    { this.quantityG = q; }
}