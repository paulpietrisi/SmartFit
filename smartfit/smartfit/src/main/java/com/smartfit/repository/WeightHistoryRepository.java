package com.smartfit.repository;

import com.smartfit.entity.User;
import com.smartfit.entity.WeightHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightHistoryRepository extends JpaRepository<WeightHistory, Long> {

    @Query("SELECT w FROM WeightHistory w " +
            "WHERE w.user = :user AND w.loggedDate >= :startDate AND w.loggedDate <= :endDate " +
            "ORDER BY w.loggedDate ASC")
    List<WeightHistory> findLast30Days(@Param("user") User user,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    // Adaugat pentru stergere istoric din Setari
    List<WeightHistory> findByUser(User user);

    // Găsește cea mai recentă înregistrare pentru un utilizator
    Optional<WeightHistory> findFirstByUserOrderByLoggedDateDesc(User user);

    // Găsește toate înregistrările pentru un utilizator într-un interval de date
    List<WeightHistory> findByUserAndLoggedDateBetweenOrderByLoggedDateAsc(User user,
                                                                           LocalDate start,
                                                                           LocalDate end);
}