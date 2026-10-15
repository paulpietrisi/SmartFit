package com.smartfit.repository;

import com.smartfit.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {

    Optional<EmployeeProfile> findByUserId(Long userId);

    Optional<EmployeeProfile> findByUser_Email(String email);

    List<EmployeeProfile> findByDepartment(String department);

    @Query("SELECT ep FROM EmployeeProfile ep JOIN FETCH ep.user u " +
           "WHERE (:department IS NULL OR ep.department = :department) " +
           "ORDER BY u.lastName, u.firstName")
    List<EmployeeProfile> searchByDepartment(@Param("department") String department);

    void deleteByUserId(Long userId);
}
