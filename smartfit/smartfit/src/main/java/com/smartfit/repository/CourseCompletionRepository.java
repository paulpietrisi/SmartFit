package com.smartfit.repository;

import com.smartfit.entity.CourseCompletion;
import com.smartfit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseCompletionRepository extends JpaRepository<CourseCompletion, Long> {

    List<CourseCompletion> findByUserId(Long userId);

    // Metoda adăugată pentru a regăsi înregistrarea specifică
    @Query("SELECT c FROM CourseCompletion c WHERE c.user.id = :userId AND c.course.id = :courseId")
    Optional<CourseCompletion> findByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // Anti-farming: verifică dacă utilizatorul a finalizat deja cursul
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CourseCompletion c " +
            "WHERE c.user.id = :userId AND c.course.id = :courseId")
    boolean existsByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // Folosit de MainController pentru statistica "X cursuri completate"
    @Query("SELECT COUNT(c) FROM CourseCompletion c WHERE c.user.id = :userId")
    Long countCompletedCoursesByUserId(@Param("userId") Long userId);

    @Query("SELECT c.title, COUNT(cc), AVG(cc.scoreObtained) FROM CourseCompletion cc JOIN cc.course c " +
            "WHERE cc.completedAt BETWEEN :from AND :to GROUP BY c.title ORDER BY COUNT(cc) DESC")
    List<Object[]> completionStatsByCourse(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT cc FROM CourseCompletion cc " +
            "JOIN cc.user u JOIN cc.course c " +
            "WHERE (:from IS NULL OR cc.completedAt >= :from) AND " +
            "(:to IS NULL OR cc.completedAt <= :to) " +
            "ORDER BY cc.completedAt DESC")
    List<CourseCompletion> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Găsește toate completările de curs pentru un utilizator specific
    List<CourseCompletion> findByUser(User user);
}