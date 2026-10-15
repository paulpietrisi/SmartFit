package com.smartfit.service;

import com.smartfit.entity.Product;
import com.smartfit.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Importă date din fișiere XLS/XLSX în baza de date.
 * Folosit din panoul admin/angajat magazin: "Import produse din Excel".
 *
 * Format așteptat (rândul 1 = header, ignorat):
 * | Nume | Categorie | Pret | Stoc | Descriere | URL imagine |
 */
@Service
public class ImportService {

    private final ProductRepository productRepository;

    public ImportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public static class ImportResult {
        public int totalRows;
        public int imported;
        public int updated;
        public List<String> errors = new ArrayList<>();

        public ImportResult() {}
    }

    /**
     * Importă produse dintr-un fișier Excel.
     * Dacă există deja un produs cu același nume + categorie → îl actualizează (preț/stoc/descriere).
     * Altfel → creează un produs nou.
     */
    public ImportResult importProducts(MultipartFile file) {
        ImportResult result = new ImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String name = formatter.formatCellValue(row.getCell(0)).trim();
                if (name.isEmpty()) continue; // rând gol, skip

                result.totalRows++;

                try {
                    String category = formatter.formatCellValue(row.getCell(1)).trim();
                    String priceStr = formatter.formatCellValue(row.getCell(2)).trim().replace(",", ".");
                    String stockStr = formatter.formatCellValue(row.getCell(3)).trim();
                    String description = row.getCell(4) != null ? formatter.formatCellValue(row.getCell(4)) : "";
                    String imageUrl = row.getCell(5) != null ? formatter.formatCellValue(row.getCell(5)) : null;

                    if (category.isEmpty()) {
                        result.errors.add("Rândul " + (rowIdx + 1) + ": categoria este obligatorie (produs: " + name + ")");
                        continue;
                    }

                    double price;
                    int stock;
                    try {
                        price = Double.parseDouble(priceStr);
                        stock = (int) Double.parseDouble(stockStr.isEmpty() ? "0" : stockStr);
                    } catch (NumberFormatException nfe) {
                        result.errors.add("Rândul " + (rowIdx + 1) + ": preț/stoc invalid pentru produsul \"" + name + "\"");
                        continue;
                    }

                    if (price <= 0) {
                        result.errors.add("Rândul " + (rowIdx + 1) + ": prețul trebuie să fie pozitiv (produs: " + name + ")");
                        continue;
                    }
                    if (stock < 0) {
                        result.errors.add("Rândul " + (rowIdx + 1) + ": stocul nu poate fi negativ (produs: " + name + ")");
                        continue;
                    }

                    // Caută produs existent cu același nume
                    Product existing = productRepository.search(name, category, null, null, null)
                            .stream()
                            .filter(p -> p.getName().equalsIgnoreCase(name))
                            .findFirst()
                            .orElse(null);

                    if (existing != null) {
                        existing.setPrice(price);
                        existing.setStockQuantity(stock);
                        if (!description.isEmpty()) existing.setDescription(description);
                        if (imageUrl != null && !imageUrl.isBlank()) existing.setImageUrl(imageUrl);
                        productRepository.save(existing);
                        result.updated++;
                    } else {
                        Product p = new Product();
                        p.setName(name);
                        p.setCategoryName(category);
                        p.setPrice(price);
                        p.setStockQuantity(stock);
                        p.setDescription(description);
                        p.setImageUrl(imageUrl);
                        productRepository.save(p);
                        result.imported++;
                    }

                } catch (Exception rowEx) {
                    result.errors.add("Rândul " + (rowIdx + 1) + ": eroare neașteptată — " + rowEx.getMessage());
                }
            }

        } catch (Exception e) {
            result.errors.add("Fișierul nu a putut fi citit: " + e.getMessage()
                    + " (verifică formatul .xlsx / .xls)");
        }

        return result;
    }
}
