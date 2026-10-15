package com.smartfit.repository;

import com.smartfit.entity.DailyLog;
import com.smartfit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    @Query("SELECT d FROM DailyLog d WHERE d.user = :user AND d.logDate = :date")
    Optional<DailyLog> findByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);

    // Adaugat pentru stergere istoric din Setari
    List<DailyLog> findByUser(User user);
}