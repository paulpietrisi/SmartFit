package com.smartfit.entity;
import jakarta.persistence.Transient;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "image_url")
    private String imageUrl;

    // Constructor gol pentru JPA
    public Product() {}

    // ── Getters & Setters ──
    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }

    public String getDescription()         { return description; }
    public void setDescription(String d)   { this.description = d; }

    public Double getPrice()               { return price; }
    public void setPrice(Double price)     { this.price = price; }

    public Integer getStockQuantity()            { return stockQuantity; }
    public void setStockQuantity(Integer sq)     { this.stockQuantity = sq; }

    public String getCategoryName()              { return categoryName; }
    public void setCategoryName(String cn)       { this.categoryName = cn; }

    public String getImageUrl()                  { return imageUrl; }
    public void setImageUrl(String imageUrl)     { this.imageUrl = imageUrl; }

    // Helper: returnează URL-ul imaginii sau null dacă nu există
    public String getDisplayImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) return imageUrl;
        return null;
    }

    // Helper: returnează emoji de fallback în funcție de categorie
    public String getFallbackEmoji() {
        if (categoryName == null) return "📦";
        return switch (categoryName) {
            case "Suplimente" -> "💊";
            case "Alimente"   -> "🥗";
            case "Vestimentatie" -> "👕";
            case "Echipament" -> "🎒";
            default -> "📦";
        };
    }

    // Helper: culoarea de fundal pentru card (folosit în Thymeleaf)
    public String getCardBg() {
        if (categoryName == null) return "#F5F5F7";
        return switch (categoryName) {
            case "Suplimente"    -> "#EAF3FF";
            case "Alimente"      -> "#FFF9EC";
            case "Vestimentatie" -> "#EAF3FF";
            case "Echipament"    -> "#E8F8EF";
            default -> "#F5F5F7";
        };
    }
}