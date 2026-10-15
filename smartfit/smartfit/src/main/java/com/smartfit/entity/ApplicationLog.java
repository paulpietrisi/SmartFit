package com.smartfit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_logs")
public class ApplicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "user_role", nullable = false, length = 20)
    private String userRole;

    @Column(name = "department", length = 20)
    private String department;

    // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, VIEW, EXPORT, IMPORT,
    // ORDER_PLACED, ORDER_STATUS_CHANGE, QUIZ_SUBMIT, COURSE_ENROLL, ERROR
    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "action_description", nullable = false, length = 500)
    private String actionDescription;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "sql_command", columnDefinition = "TEXT")
    private String sqlCommand;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "logged_at")
    private LocalDateTime loggedAt;

    @PrePersist
    public void prePersist() {
        if (loggedAt == null) loggedAt = LocalDateTime.now();
    }

    public ApplicationLog() {}

    // ── Getters & Setters ──
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public Long getUserId()                      { return userId; }
    public void setUserId(Long v)                { this.userId = v; }
    public String getUserEmail()                 { return userEmail; }
    public void setUserEmail(String v)           { this.userEmail = v; }
    public String getUserRole()                  { return userRole; }
    public void setUserRole(String v)            { this.userRole = v; }
    public String getDepartment()                { return department; }
    public void setDepartment(String v)          { this.department = v; }
    public String getActionType()                { return actionType; }
    public void setActionType(String v)          { this.actionType = v; }
    public String getActionDescription()         { return actionDescription; }
    public void setActionDescription(String v)   { this.actionDescription = v; }
    public String getEntityType()                { return entityType; }
    public void setEntityType(String v)          { this.entityType = v; }
    public Long getEntityId()                    { return entityId; }
    public void setEntityId(Long v)              { this.entityId = v; }
    public String getSqlCommand()                { return sqlCommand; }
    public void setSqlCommand(String v)          { this.sqlCommand = v; }
    public String getIpAddress()                 { return ipAddress; }
    public void setIpAddress(String v)           { this.ipAddress = v; }
    public String getUserAgent()                 { return userAgent; }
    public void setUserAgent(String v)           { this.userAgent = v; }
    public LocalDateTime getLoggedAt()           { return loggedAt; }
    public void setLoggedAt(LocalDateTime v)     { this.loggedAt = v; }
}
