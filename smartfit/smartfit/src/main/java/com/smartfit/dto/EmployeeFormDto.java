package com.smartfit.dto;

import jakarta.validation.constraints.*;

/**
 * DTO folosit de ADMIN pentru creare/editare angajați (academie/magazin).
 */
public class EmployeeFormDto {

    private Long userId; // null = angajat nou

    @NotBlank(message = "Emailul este obligatoriu")
    @Email(message = "Email invalid")
    private String email;

    /** Doar la creare; la editare se lasă gol pentru a nu schimba parola. */
    @Size(min = 0, max = 100, message = "Parola este prea lungă")
    private String password;

    @NotBlank(message = "Prenumele este obligatoriu")
    private String firstName;

    @NotBlank(message = "Numele este obligatoriu")
    private String lastName;

    @NotBlank(message = "Departamentul este obligatoriu")
    private String department; // ACADEMY / SHOP

    private Boolean canCreate = true;
    private Boolean canEdit = true;
    private Boolean canDelete = false;

    private String hiredDate; // yyyy-MM-dd, opțional

    @Size(max = 1000, message = "Notele pot avea maxim 1000 caractere")
    private String notes;

    public EmployeeFormDto() {}

    // ── Getters & Setters ──
    public Long getUserId()                  { return userId; }
    public void setUserId(Long v)            { this.userId = v; }
    public String getEmail()                 { return email; }
    public void setEmail(String email)       { this.email = email; }
    public String getPassword()              { return password; }
    public void setPassword(String v)        { this.password = v; }
    public String getFirstName()             { return firstName; }
    public void setFirstName(String v)       { this.firstName = v; }
    public String getLastName()              { return lastName; }
    public void setLastName(String v)        { this.lastName = v; }
    public String getDepartment()            { return department; }
    public void setDepartment(String v)      { this.department = v; }
    public Boolean getCanCreate()            { return canCreate; }
    public void setCanCreate(Boolean v)      { this.canCreate = v; }
    public Boolean getCanEdit()              { return canEdit; }
    public void setCanEdit(Boolean v)        { this.canEdit = v; }
    public Boolean getCanDelete()            { return canDelete; }
    public void setCanDelete(Boolean v)      { this.canDelete = v; }
    public String getHiredDate()             { return hiredDate; }
    public void setHiredDate(String v)       { this.hiredDate = v; }
    public String getNotes()                 { return notes; }
    public void setNotes(String notes)       { this.notes = notes; }
}
