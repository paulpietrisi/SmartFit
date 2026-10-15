package com.smartfit.repository;

import com.smartfit.entity.Order;
import com.smartfit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Filtrare pentru pagina de rapoarte / comenzi (admin & angajat magazin).
     */
    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:userEmail IS NULL OR LOWER(o.contactEmail) LIKE LOWER(CONCAT('%',:userEmail,'%'))) AND " +
            "(:from IS NULL OR o.createdAt >= :from) AND " +
            "(:to IS NULL OR o.createdAt <= :to) AND " +
            "(:minTotal IS NULL OR o.totalPrice >= :minTotal) AND " +
            "(:maxTotal IS NULL OR o.totalPrice <= :maxTotal) " +
            "ORDER BY o.createdAt DESC")
    List<Order> search(@Param("status") String status,
                       @Param("userEmail") String userEmail,
                       @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to,
                       @Param("minTotal") Double minTotal,
                       @Param("maxTotal") Double maxTotal);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC")
    List<Order> findByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.totalPrice),0) FROM Order o " +
            "WHERE o.createdAt BETWEEN :from AND :to AND o.status <> 'CANCELLED'")
    Double sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :from AND :to")
    Long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Vânzări pe categorie de produs, într-o perioadă — pentru graficul de pe rapoarte.
     * Returnează: [categoryName, sumValue, totalQty]
     */
    @Query("SELECT p.categoryName, SUM(oi.priceAtPurchase * oi.quantity), SUM(oi.quantity) " +
            "FROM OrderItem oi " +
            "JOIN oi.product p " +
            "JOIN oi.order o " +
            "WHERE o.createdAt BETWEEN :from AND :to AND o.status <> 'CANCELLED' " +
            "GROUP BY p.categoryName " +
            "ORDER BY SUM(oi.priceAtPurchase * oi.quantity) DESC")
    List<Object[]> salesByCategory(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Top produse vândute într-o perioadă.
     */
    @Query("SELECT p.name, SUM(oi.quantity), SUM(oi.priceAtPurchase * oi.quantity) " +
            "FROM OrderItem oi " +
            "JOIN oi.product p " +
            "JOIN oi.order o " +
            "WHERE o.createdAt BETWEEN :from AND :to AND o.status <> 'CANCELLED' " +
            "GROUP BY p.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> topProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Evoluție vânzări pe zi, pentru graficul liniar din rapoarte.
     */
    @Query("SELECT FUNCTION('DATE', o.createdAt), SUM(o.totalPrice), COUNT(o) " +
            "FROM Order o " +
            "WHERE o.createdAt BETWEEN :from AND :to AND o.status <> 'CANCELLED' " +
            "GROUP BY FUNCTION('DATE', o.createdAt) " +
            "ORDER BY FUNCTION('DATE', o.createdAt)")
    List<Object[]> dailySales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Distribuție comenzi pe status, pentru pie chart.
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o " +
            "WHERE o.createdAt BETWEEN :from AND :to GROUP BY o.status")
    List<Object[]> statusDistribution(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Găsește toate comenzile pentru un utilizator specific
    List<Order> findByUser(User user);

    // Găsește toate comenzile pentru un utilizator într-un interval de timp
    List<Order> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);
}