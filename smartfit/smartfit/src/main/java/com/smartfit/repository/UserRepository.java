package com.smartfit.repository;

import com.smartfit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Găsește utilizatorul după email
    User findByEmail(String email);

    // Găsește utilizatorii după rol (folosit în ReportService)
    List<User> findByRole(String role);

    // Verifică dacă există deja email-ul în bază (folosit în EmployeeService)
    boolean existsByEmail(String email);

    // Returnează top 10 utilizatori după puncte (folosit în AcademieController pentru leaderboard)
    List<User> findTop10ByOrderByFitPointsDesc();

    // Căutare complexă cu filtre opționale (folosit în AdminController)
    @Query("SELECT u FROM User u WHERE " +
            "(:q IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:goal IS NULL OR u.goal = :goal) AND " +
            "(:gender IS NULL OR u.gender = :gender)")
    List<User> search(@Param("q") String q,
                      @Param("role") String role,
                      @Param("goal") String goal,
                      @Param("gender") String gender);
}