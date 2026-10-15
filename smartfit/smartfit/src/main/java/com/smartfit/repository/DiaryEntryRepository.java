package com.smartfit.repository;

import com.smartfit.entity.DiaryEntry;
import com.smartfit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {

    @Query("SELECT d FROM DiaryEntry d JOIN FETCH d.food " +
            "WHERE d.user = :user AND d.consumedDate = :date")
    List<DiaryEntry> findByUserAndDate(@Param("user") User user,
                                       @Param("date") LocalDate date);

    @Query("SELECT d.consumedDate, " +
            "SUM(d.food.kcalPer100g * d.quantityG / 100.0) " +
            "FROM DiaryEntry d " +
            "WHERE d.user = :user AND d.consumedDate >= :startDate AND d.consumedDate <= :endDate " +
            "GROUP BY d.consumedDate " +
            "ORDER BY d.consumedDate ASC")
    List<Object[]> findDailyKcalLast30Days(@Param("user") User user,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    // Adaugat pentru stergere istoric din Setari
    List<DiaryEntry> findByUser(User user);

    // Adaugat pentru export (Setari) — incarca eager food, ordonat descrescator
    @Query("SELECT d FROM DiaryEntry d JOIN FETCH d.food WHERE d.user = :user ORDER BY d.consumedDate DESC")
    List<DiaryEntry> findByUserWithFoodOrderByDateDesc(@Param("user") User user);

    // Găsește toate intrările pentru un utilizator la o anumită dată
    List<DiaryEntry> findByUserAndConsumedDate(User user, LocalDate date);

    // Găsește toate intrările pentru un utilizator într-un interval de date, ordonate crescător după dată
    List<DiaryEntry> findByUserAndConsumedDateBetweenOrderByConsumedDateAsc(User user,
                                                                            LocalDate start,
                                                                            LocalDate end);
}