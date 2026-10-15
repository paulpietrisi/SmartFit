package com.smartfit.repository;

import com.smartfit.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    List<CourseEnrollment> findByUserId(Long userId);

    Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    // Metoda adăugată pentru a verifica existența unei înscrieri fără a încărca entitatea
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // Folosit de AcademieController pentru a verifica/actualiza progresul unei înscrieri
    @Query("SELECT e FROM CourseEnrollment e WHERE e.user.id = :userId AND e.course.id = :courseId")
    Optional<CourseEnrollment> findByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    Long countByCourseId(Long courseId);

    @Query("SELECT c.title, COUNT(e) FROM CourseEnrollment e JOIN e.course c GROUP BY c.title ORDER BY COUNT(e) DESC")
    List<Object[]> enrollmentCountPerCourse();
}