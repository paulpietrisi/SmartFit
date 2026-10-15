package com.smartfit.repository;

import com.smartfit.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByCourseId(Long courseId);

    // Folosit de AcademieController — întrebările unui curs, în ordinea corectă pentru quiz
    @Query("SELECT q FROM QuizQuestion q WHERE q.course.id = :courseId ORDER BY q.id ASC")
    List<QuizQuestion> findByCourseIdOrdered(@Param("courseId") Long courseId);

    void deleteByCourseId(Long courseId);
}