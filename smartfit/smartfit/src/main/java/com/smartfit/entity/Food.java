package com.smartfit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "foods")
public class Food {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "kcal_per_100g")
    private Double kcalPer100g;

    private Double protein;
    private Double carbs;
    private Double fat;

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public Double getKcalPer100g(){ return kcalPer100g; }
    public Double getProtein()    { return protein; }
    public Double getCarbs()      { return carbs; }
    public Double getFat()        { return fat; }
}