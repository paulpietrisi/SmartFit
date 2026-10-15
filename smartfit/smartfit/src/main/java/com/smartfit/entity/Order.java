package com.smartfit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "points_used")
    private Integer pointsUsed = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Hibernate adaugă aceste coloane automat (ddl-auto=update)
    @Column(name = "payment_method", length = 10)
    private String paymentMethod;

    @Column(name = "stripe_payment_id", length = 255)
    private String stripePaymentId;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    // ===== AICI ESTE MODIFICAREA SALVATOARE: fetch = FetchType.EAGER =====
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public User getUser()                       { return user; }
    public void setUser(User user)              { this.user = user; }
    public Double getTotalPrice()               { return totalPrice; }
    public void setTotalPrice(Double v)         { this.totalPrice = v; }
    public String getStatus()                   { return status; }
    public void setStatus(String v)             { this.status = v; }
    public String getShippingAddress()          { return shippingAddress; }
    public void setShippingAddress(String v)    { this.shippingAddress = v; }
    public Integer getPointsUsed()              { return pointsUsed; }
    public void setPointsUsed(Integer v)        { this.pointsUsed = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
    public String getPaymentMethod()            { return paymentMethod; }
    public void setPaymentMethod(String v)      { this.paymentMethod = v; }
    public String getStripePaymentId()          { return stripePaymentId; }
    public void setStripePaymentId(String v)    { this.stripePaymentId = v; }
    public String getFullName()                 { return fullName; }
    public void setFullName(String v)           { this.fullName = v; }
    public String getPhone()                    { return phone; }
    public void setPhone(String v)              { this.phone = v; }
    public String getContactEmail()             { return contactEmail; }
    public void setContactEmail(String v)       { this.contactEmail = v; }
    public List<OrderItem> getItems()           { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}