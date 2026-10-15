package com.smartfit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_purchase", nullable = false)
    private Double priceAtPurchase;

    public OrderItem() {}

    // ── Getters & Setters ──
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public Order getOrder()                     { return order; }
    public void setOrder(Order order)           { this.order = order; }
    public Product getProduct()                 { return product; }
    public void setProduct(Product product)     { this.product = product; }
    public Integer getQuantity()                { return quantity; }
    public void setQuantity(Integer quantity)   { this.quantity = quantity; }
    public Double getPriceAtPurchase()          { return priceAtPurchase; }
    public void setPriceAtPurchase(Double v)    { this.priceAtPurchase = v; }

    public double getLineTotal() {
        return priceAtPurchase != null && quantity != null
                ? priceAtPurchase * quantity : 0;
    }
}