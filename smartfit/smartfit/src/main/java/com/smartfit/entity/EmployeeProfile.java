package com.smartfit.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_profiles")
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Valori valide: 'ACADEMY', 'SHOP'
    @Column(nullable = false, length = 20)
    private String department;

    @Column(name = "can_create")
    private Boolean canCreate = true;

    @Column(name = "can_edit")
    private Boolean canEdit = true;

    @Column(name = "can_delete")
    private Boolean canDelete = false;

    @Column(name = "hired_date")
    private LocalDate hiredDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public EmployeeProfile() {}

    // ── Getters & Setters ──
    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }
    public User getUser()                  { return user; }
    public void setUser(User user)         { this.user = user; }
    public String getDepartment()          { return department; }
    public void setDepartment(String d)    { this.department = d; }
    public Boolean getCanCreate()          { return canCreate; }
    public void setCanCreate(Boolean v)    { this.canCreate = v; }
    public Boolean getCanEdit()            { return canEdit; }
    public void setCanEdit(Boolean v)      { this.canEdit = v; }
    public Boolean getCanDelete()          { return canDelete; }
    public void setCanDelete(Boolean v)    { this.canDelete = v; }
    public LocalDate getHiredDate()        { return hiredDate; }
    public void setHiredDate(LocalDate v)  { this.hiredDate = v; }
    public String getNotes()               { return notes; }
    public void setNotes(String notes)     { this.notes = notes; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    // ── Helper ──
    public boolean isAcademy() { return "ACADEMY".equals(department); }
    public boolean isShop()    { return "SHOP".equals(department); }
}
