package com.smartfit.dto;

import jakarta.validation.constraints.*;

/**
 * DTO folosit la creare/editare produs din panoul admin/angajat magazin.
 * Validările asigură funcționarea fără erori a formularului.
 */
public class ProductFormDto {

    private Long id;

    @NotBlank(message = "Numele produsului este obligatoriu")
    @Size(max = 150, message = "Numele poate avea maxim 150 caractere")
    private String name;

    @Size(max = 2000, message = "Descrierea poate avea maxim 2000 caractere")
    private String description;

    @NotNull(message = "Prețul este obligatoriu")
    @DecimalMin(value = "0.01", message = "Prețul trebuie să fie mai mare decât 0")
    @Digits(integer = 6, fraction = 2, message = "Format preț invalid (max 2 zecimale)")
    private Double price;

    @NotNull(message = "Stocul este obligatoriu")
    @Min(value = 0, message = "Stocul nu poate fi negativ")
    private Integer stockQuantity;

    @NotBlank(message = "Categoria este obligatorie")
    private String categoryName;

    private String imageUrl;

    public ProductFormDto() {}

    // ── Getters & Setters ──
    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public String getDescription()            { return description; }
    public void setDescription(String v)      { this.description = v; }
    public Double getPrice()                  { return price; }
    public void setPrice(Double price)        { this.price = price; }
    public Integer getStockQuantity()         { return stockQuantity; }
    public void setStockQuantity(Integer v)   { this.stockQuantity = v; }
    public String getCategoryName()           { return categoryName; }
    public void setCategoryName(String v)     { this.categoryName = v; }
    public String getImageUrl()               { return imageUrl; }
    public void setImageUrl(String v)         { this.imageUrl = v; }
}
