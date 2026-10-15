package com.smartfit.repository;

import com.smartfit.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryName(String categoryName);

    @Query("SELECT DISTINCT p.categoryName FROM Product p ORDER BY p.categoryName")
    List<String> findAllCategories();

    /**
     * Căutare + filtrare combinată (folosită în admin/magazin angajat).
     * Toți parametrii opționali — NULL = ignorat.
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%'))) AND " +
           "(:category IS NULL OR p.categoryName = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:inStock IS NULL OR (:inStock = TRUE AND p.stockQuantity > 0) " +
           "                   OR (:inStock = FALSE AND p.stockQuantity = 0)) " +
           "ORDER BY p.id DESC")
    List<Product> search(@Param("q") String q,
                          @Param("category") String category,
                          @Param("minPrice") Double minPrice,
                          @Param("maxPrice") Double maxPrice,
                          @Param("inStock") Boolean inStock);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= :threshold ORDER BY p.stockQuantity ASC")
    List<Product> findLowStock(@Param("threshold") Integer threshold);
}
