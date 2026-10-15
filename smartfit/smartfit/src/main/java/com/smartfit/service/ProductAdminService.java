package com.smartfit.service;

import com.smartfit.dto.ProductFormDto;
import com.smartfit.entity.Product;
import com.smartfit.entity.User;
import com.smartfit.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Logică CRUD pentru produse (Magazin), folosită de AdminController
 * și de EmployeeShopController. Toate operațiile de scriere sunt logate.
 */
@Service
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public ProductAdminService(ProductRepository productRepository, AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    // ── LISTARE / CĂUTARE / FILTRARE ──────────────────────────
    public List<Product> search(String q, String category, Double minPrice, Double maxPrice, Boolean inStock) {
        return productRepository.search(
                blank(q), blank(category), minPrice, maxPrice, inStock);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produsul cu ID " + id + " nu a fost găsit"));
    }

    public List<Product> findLowStock(int threshold) {
        return productRepository.findLowStock(threshold);
    }

    // ── ADAUGARE ───────────────────────────────────────────────
    public Product create(ProductFormDto dto, User actor, HttpServletRequest request) {
        Product p = new Product();
        applyDto(p, dto);
        Product saved = productRepository.save(p);

        auditLogService.log(actor, "CREATE",
                "Produs adăugat: " + saved.getName() + " (" + saved.getCategoryName() + ")",
                "products", saved.getId(),
                buildInsertSql(saved), request);

        return saved;
    }

    // ── MODIFICARE ─────────────────────────────────────────────
    public Product update(Long id, ProductFormDto dto, User actor, HttpServletRequest request) {
        Product p = findById(id);
        String oldSummary = String.format(Locale.ROOT, "preț=%.2f, stoc=%d", p.getPrice(), p.getStockQuantity());

        applyDto(p, dto);
        Product saved = productRepository.save(p);

        String newSummary = String.format(Locale.ROOT, "preț=%.2f, stoc=%d", saved.getPrice(), saved.getStockQuantity());

        auditLogService.log(actor, "UPDATE",
                "Produs modificat: " + saved.getName() + " (" + oldSummary + " → " + newSummary + ")",
                "products", saved.getId(),
                buildUpdateSql(saved), request);

        return saved;
    }

    // ── ȘTERGERE ───────────────────────────────────────────────
    public void delete(Long id, User actor, HttpServletRequest request) {
        Product p = findById(id);
        String name = p.getName();

        productRepository.deleteById(id);

        auditLogService.log(actor, "DELETE",
                "Produs șters: " + name,
                "products", id,
                "DELETE FROM products WHERE id = " + id, request);
    }

    // ── helpers ─────────────────────────────────────────────────
    private void applyDto(Product p, ProductFormDto dto) {
        p.setName(dto.getName().trim());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setStockQuantity(dto.getStockQuantity());
        p.setCategoryName(dto.getCategoryName().trim());
        p.setImageUrl(dto.getImageUrl());
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String buildInsertSql(Product p) {
        return String.format(Locale.ROOT,
                "INSERT INTO products (name, description, price, stock_quantity, category_name, image_url) " +
                "VALUES ('%s', '...', %.2f, %d, '%s', '%s')",
                escape(p.getName()), p.getPrice(), p.getStockQuantity(),
                escape(p.getCategoryName()), escape(p.getImageUrl()));
    }

    private String buildUpdateSql(Product p) {
        return String.format(Locale.ROOT,
                "UPDATE products SET name='%s', price=%.2f, stock_quantity=%d, category_name='%s' WHERE id=%d",
                escape(p.getName()), p.getPrice(), p.getStockQuantity(),
                escape(p.getCategoryName()), p.getId());
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("'", "\\'");
    }
}
