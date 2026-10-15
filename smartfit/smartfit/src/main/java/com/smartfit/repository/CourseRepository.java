package com.smartfit.repository;

import com.smartfit.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // Folosit de pagina publică Academie (ordine fixă de afișare)
    List<Course> findAllByOrderByIdAsc();

    @Query("SELECT c FROM Course c WHERE " +
            "(:q IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
            "          OR LOWER(c.description) LIKE LOWER(CONCAT('%',:q,'%'))) AND " +
            "(:difficulty IS NULL OR c.difficulty = :difficulty) AND " +
            "(:minPoints IS NULL OR c.rewardPoints >= :minPoints) AND " +
            "(:maxPoints IS NULL OR c.rewardPoints <= :maxPoints) " +
            "ORDER BY c.id ASC")
    List<Course> search(@Param("q") String q,
                        @Param("difficulty") String difficulty,
                        @Param("minPoints") Integer minPoints,
                        @Param("maxPoints") Integer maxPoints);

    @Query("SELECT DISTINCT c.difficulty FROM Course c")
    List<String> findAllDifficulties();
}