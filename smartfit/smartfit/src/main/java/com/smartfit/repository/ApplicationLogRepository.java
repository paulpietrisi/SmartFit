package com.smartfit.repository;

import com.smartfit.entity.ApplicationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {

    /**
     * Filtrare avansată pentru pagina de log-uri / rapoarte.
     * Toți parametrii sunt opționali (NULL = nu se aplică filtrul).
     */
    @Query("SELECT l FROM ApplicationLog l WHERE " +
           "(:email IS NULL OR LOWER(l.userEmail) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:role IS NULL OR l.userRole = :role) AND " +
           "(:department IS NULL OR l.department = :department) AND " +
           "(:actionType IS NULL OR l.actionType = :actionType) AND " +
           "(:entityType IS NULL OR l.entityType = :entityType) AND " +
           "(:from IS NULL OR l.loggedAt >= :from) AND " +
           "(:to IS NULL OR l.loggedAt <= :to) " +
           "ORDER BY l.loggedAt DESC")
    Page<ApplicationLog> search(
            @Param("email") String email,
            @Param("role") String role,
            @Param("department") String department,
            @Param("actionType") String actionType,
            @Param("entityType") String entityType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT l FROM ApplicationLog l WHERE " +
           "(:email IS NULL OR LOWER(l.userEmail) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:role IS NULL OR l.userRole = :role) AND " +
           "(:department IS NULL OR l.department = :department) AND " +
           "(:actionType IS NULL OR l.actionType = :actionType) AND " +
           "(:entityType IS NULL OR l.entityType = :entityType) AND " +
           "(:from IS NULL OR l.loggedAt >= :from) AND " +
           "(:to IS NULL OR l.loggedAt <= :to) " +
           "ORDER BY l.loggedAt DESC")
    List<ApplicationLog> searchAll(
            @Param("email") String email,
            @Param("role") String role,
            @Param("department") String department,
            @Param("actionType") String actionType,
            @Param("entityType") String entityType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<ApplicationLog> findTop50ByOrderByLoggedAtDesc();
}
