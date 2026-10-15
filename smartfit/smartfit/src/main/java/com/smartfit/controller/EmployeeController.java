package com.smartfit.controller;

import com.smartfit.dto.CourseFormDto;
import com.smartfit.dto.ProductFormDto;
import com.smartfit.entity.*;
import com.smartfit.repository.EmployeeProfileRepository;
import com.smartfit.repository.UserRepository;
import com.smartfit.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Panou angajat — accesibil de ROLE_EMPLOYEE (vezi SecurityConfig: /angajat/** → ROLE_EMPLOYEE).
 *
 * Un angajat are un EmployeeProfile cu:
 *  - department = ACADEMY → vede /angajat/cursuri
 *  - department = SHOP    → vede /angajat/produse + /angajat/comenzi
 *
 * Permisiunile can_create / can_edit / can_delete (setate de ADMIN) restricționează
 * butoanele de Adăugare/Editare/Ștergere — verificate atât în view (HTML) cât și aici,
 * server-side, pentru siguranță.
 */
@Controller
@RequestMapping("/angajat")
public class EmployeeController {

    private final ProductAdminService productAdminService;
    private final CourseAdminService courseAdminService;
    private final OrderAdminHelper orderAdminHelper;
    private final ReportService reportService;
    private final ExportService exportService;
    private final ImportService importService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public EmployeeController(ProductAdminService productAdminService,
                               CourseAdminService courseAdminService,
                               OrderAdminHelper orderAdminHelper,
                               ReportService reportService,
                               ExportService exportService,
                               ImportService importService,
                               AuditLogService auditLogService,
                               UserRepository userRepository,
                               EmployeeProfileRepository employeeProfileRepository) {
        this.productAdminService = productAdminService;
        this.courseAdminService = courseAdminService;
        this.orderAdminHelper = orderAdminHelper;
        this.reportService = reportService;
        this.exportService = exportService;
        this.importService = importService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
    }

    // ════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════
    private User currentUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername());
    }

    private EmployeeProfile currentProfile(UserDetails principal) {
        User user = currentUser(principal);
        return employeeProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Acest cont nu are un profil de angajat configurat. Contactează administratorul."));
    }

    /** Verifică permisiunea și aruncă excepție lizibilă dacă nu există. */
    private void requirePermission(EmployeeProfile ep, String action, boolean allowed) {
        if (!allowed) {
            throw new SecurityException("Nu ai permisiunea de a " + action
                    + ". Contactează administratorul pentru a obține acces.");
        }
    }

    // ════════════════════════════════════════════════════════════
    // DASHBOARD ANGAJAT — redirecționează în funcție de departament
    // ════════════════════════════════════════════════════════════
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        EmployeeProfile ep = currentProfile(principal);
        model.addAttribute("profile", ep);

        if (ep.isAcademy()) {
            model.addAttribute("courseCount", courseAdminService.findAll().size());
            return "angajat-dashboard";
        } else {
            model.addAttribute("productCount", productAdminService.findAll().size());
            model.addAttribute("lowStock", productAdminService.findLowStock(10));
            return "angajat-dashboard";
        }
    }

    // ════════════════════════════════════════════════════════════
    // SHOP — PRODUSE (listare / căutare / filtrare / CRUD restricționat)
    // ════════════════════════════════════════════════════════════
    @GetMapping("/produse")
    public String products(@AuthenticationPrincipal UserDetails principal,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) Double minPrice,
                            @RequestParam(required = false) Double maxPrice,
                            @RequestParam(required = false) Boolean inStock,
                            Model model) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) {
            throw new SecurityException("Acces interzis: această secțiune este pentru angajații Magazin.");
        }

        model.addAttribute("products", productAdminService.search(q, category, minPrice, maxPrice, inStock));
        model.addAttribute("categories", productAdminService.findAllCategories());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("inStock", inStock);
        model.addAttribute("profile", ep);
        return "angajat-produse";
    }

    @GetMapping("/produse/nou")
    public String newProductForm(@AuthenticationPrincipal UserDetails principal, Model model) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "adăuga produse", Boolean.TRUE.equals(ep.getCanCreate()));

        model.addAttribute("product", new ProductFormDto());
        model.addAttribute("categories", productAdminService.findAllCategories());
        model.addAttribute("isNew", true);
        model.addAttribute("profile", ep);
        return "angajat-produs-form";
    }

    @GetMapping("/produse/{id}/edit")
    public String editProductForm(@AuthenticationPrincipal UserDetails principal,
                                   @PathVariable Long id, Model model) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "modifica produse", Boolean.TRUE.equals(ep.getCanEdit()));

        Product p = productAdminService.findById(id);
        ProductFormDto dto = new ProductFormDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setStockQuantity(p.getStockQuantity());
        dto.setCategoryName(p.getCategoryName());
        dto.setImageUrl(p.getImageUrl());

        model.addAttribute("product", dto);
        model.addAttribute("categories", productAdminService.findAllCategories());
        model.addAttribute("isNew", false);
        model.addAttribute("profile", ep);
        return "angajat-produs-form";
    }

    @PostMapping("/produse/salveaza")
    public String saveProduct(@AuthenticationPrincipal UserDetails principal,
                               @Valid @ModelAttribute("product") ProductFormDto dto,
                               BindingResult bindingResult,
                               HttpServletRequest request,
                               Model model,
                               RedirectAttributes ra) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");

        boolean isNew = dto.getId() == null;
        requirePermission(ep, isNew ? "adăuga produse" : "modifica produse",
                isNew ? Boolean.TRUE.equals(ep.getCanCreate()) : Boolean.TRUE.equals(ep.getCanEdit()));

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", productAdminService.findAllCategories());
            model.addAttribute("isNew", isNew);
            model.addAttribute("profile", ep);
            return "angajat-produs-form";
        }

        try {
            if (isNew) {
                productAdminService.create(dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Produs adăugat cu succes!");
            } else {
                productAdminService.update(dto.getId(), dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Produs actualizat cu succes!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/angajat/produse";
    }

    @PostMapping("/produse/{id}/sterge")
    public String deleteProduct(@AuthenticationPrincipal UserDetails principal,
                                 @PathVariable Long id,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "șterge produse", Boolean.TRUE.equals(ep.getCanDelete()));

        try {
            productAdminService.delete(id, currentUser(principal), request);
            ra.addFlashAttribute("success", "Produs șters cu succes!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Nu s-a putut șterge produsul: " + e.getMessage());
        }
        return "redirect:/angajat/produse";
    }

    // ── Export / Import produse (disponibil pentru orice angajat SHOP, doar lectură) ──
    @GetMapping("/produse/export/excel")
    public ResponseEntity<ByteArrayResource> exportProductsExcel(@AuthenticationPrincipal UserDetails principal,
                                                                   HttpServletRequest request) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");

        List<Product> products = productAdminService.findAll();
        byte[] data = exportService.exportProductsExcel(products);

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export Excel produse (" + products.size() + " produse)",
                "products", null, null, request);

        return fileResponse(data, "produse_smartfit.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @PostMapping("/produse/import")
    public String importProducts(@AuthenticationPrincipal UserDetails principal,
                                  @RequestParam("file") MultipartFile file,
                                  HttpServletRequest request,
                                  RedirectAttributes ra) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "importa produse", Boolean.TRUE.equals(ep.getCanCreate()));

        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Nu a fost selectat niciun fișier.");
            return "redirect:/angajat/produse";
        }

        ImportService.ImportResult result = importService.importProducts(file);

        auditLogService.log(currentUser(principal), "IMPORT",
                "Import Excel produse: " + result.imported + " adăugate, " + result.updated
                        + " actualizate, " + result.errors.size() + " erori",
                "products", null, null, request);

        if (result.errors.isEmpty()) {
            ra.addFlashAttribute("success", "Import finalizat: " + result.imported
                    + " produse noi, " + result.updated + " actualizate.");
        } else {
            ra.addFlashAttribute("warning", "Import cu erori: " + String.join(" | ", result.errors));
        }

        return "redirect:/angajat/produse";
    }

    // ════════════════════════════════════════════════════════════
    // SHOP — COMENZI (vizualizare + schimbare status)
    // ════════════════════════════════════════════════════════════
    @GetMapping("/comenzi")
    public String orders(@AuthenticationPrincipal UserDetails principal,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String email,
                          Model model) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");

        model.addAttribute("orders", orderAdminHelper.search(status, email, null, null, null, null));
        model.addAttribute("status", status);
        model.addAttribute("email", email);
        model.addAttribute("statuses", List.of("PENDING", "SHIPPED", "DELIVERED", "CANCELLED"));
        model.addAttribute("profile", ep);
        return "angajat-comenzi";
    }

    @PostMapping("/comenzi/{id}/status")
    public String updateOrderStatus(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long id,
                                     @RequestParam String status,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "modifica statusul comenzilor", Boolean.TRUE.equals(ep.getCanEdit()));

        try {
            orderAdminHelper.updateStatus(id, status, currentUser(principal), request);
            ra.addFlashAttribute("success", "Status comandă actualizat: " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/angajat/comenzi";
    }

    @GetMapping("/comenzi/{id}/factura")
    public ResponseEntity<ByteArrayResource> downloadInvoice(@AuthenticationPrincipal UserDetails principal,
                                                               @PathVariable Long id,
                                                               HttpServletRequest request) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isShop()) throw new SecurityException("Acces interzis.");

        Order order = orderAdminHelper.getOrder(id);
        byte[] pdf = exportService.exportInvoicePdf(order);

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export factură PDF pentru comanda #ORD-" + id,
                "orders", id, null, request);

        return fileResponse(pdf, "Factura_ORD-" + id + ".pdf", "application/pdf");
    }

    // ════════════════════════════════════════════════════════════
    // ACADEMY — CURSURI (listare / căutare / filtrare / CRUD restricționat)
    // ════════════════════════════════════════════════════════════
    @GetMapping("/cursuri")
    public String courses(@AuthenticationPrincipal UserDetails principal,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String difficulty,
                           @RequestParam(required = false) Integer minPoints,
                           @RequestParam(required = false) Integer maxPoints,
                           Model model) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isAcademy()) throw new SecurityException("Acces interzis: această secțiune este pentru angajații Academiei.");

        model.addAttribute("courses", courseAdminService.search(q, difficulty, minPoints, maxPoints));
        model.addAttribute("difficulties", courseAdminService.findAllDifficulties());
        model.addAttribute("q", q);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("minPoints", minPoints);
        model.addAttribute("maxPoints", maxPoints);
        model.addAttribute("profile", ep);
        return "angajat-cursuri";
    }

    @GetMapping("/cursuri/nou")
    public String newCourseForm(@AuthenticationPrincipal UserDetails principal, Model model) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isAcademy()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "adăuga cursuri", Boolean.TRUE.equals(ep.getCanCreate()));

        CourseFormDto dto = new CourseFormDto();
        dto.setRewardPoints(50);
        dto.setPassScorePercentage(60);
        dto.setDifficulty("Începător");
        model.addAttribute("course", dto);
        model.addAttribute("isNew", true);
        model.addAttribute("profile", ep);
        return "angajat-curs-form";
    }

    @GetMapping("/cursuri/{id}/edit")
    public String editCourseForm(@AuthenticationPrincipal UserDetails principal,
                                  @PathVariable Long id, Model model) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isAcademy()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "modifica cursuri", Boolean.TRUE.equals(ep.getCanEdit()));

        Course c = courseAdminService.findById(id);
        model.addAttribute("course", courseAdminService.toFormDto(c));
        model.addAttribute("isNew", false);
        model.addAttribute("profile", ep);
        return "angajat-curs-form";
    }

    @PostMapping("/cursuri/salveaza")
    public String saveCourse(@AuthenticationPrincipal UserDetails principal,
                              @Valid @ModelAttribute("course") CourseFormDto dto,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes ra) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isAcademy()) throw new SecurityException("Acces interzis.");

        boolean isNew = dto.getId() == null;
        requirePermission(ep, isNew ? "adăuga cursuri" : "modifica cursuri",
                isNew ? Boolean.TRUE.equals(ep.getCanCreate()) : Boolean.TRUE.equals(ep.getCanEdit()));

        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", isNew);
            model.addAttribute("profile", ep);
            return "angajat-curs-form";
        }

        try {
            if (isNew) {
                courseAdminService.create(dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Curs adăugat cu succes!");
            } else {
                courseAdminService.update(dto.getId(), dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Curs actualizat cu succes!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/angajat/cursuri";
    }

    @PostMapping("/cursuri/{id}/sterge")
    public String deleteCourse(@AuthenticationPrincipal UserDetails principal,
                                @PathVariable Long id,
                                HttpServletRequest request,
                                RedirectAttributes ra) {
        EmployeeProfile ep = currentProfile(principal);
        if (!ep.isAcademy()) throw new SecurityException("Acces interzis.");
        requirePermission(ep, "șterge cursuri", Boolean.TRUE.equals(ep.getCanDelete()));

        try {
            courseAdminService.delete(id, currentUser(principal), request);
            ra.addFlashAttribute("success", "Curs șters cu succes!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Nu s-a putut șterge cursul: " + e.getMessage());
        }
        return "redirect:/angajat/cursuri";
    }

    // ════════════════════════════════════════════════════════════
    // RAPOARTE (citire) — fiecare departament își vede statisticile proprii
    // ════════════════════════════════════════════════════════════
    @GetMapping("/rapoarte")
    public String reports(@AuthenticationPrincipal UserDetails principal,
                           @RequestParam(required = false)
                           @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                           LocalDate from,
                           @RequestParam(required = false)
                           @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                           LocalDate to,
                           @RequestParam(required = false) String status,
                           Model model) {
        EmployeeProfile ep = currentProfile(principal);

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("status", status);
        model.addAttribute("statuses", List.of("PENDING", "SHIPPED", "DELIVERED", "CANCELLED"));
        model.addAttribute("profile", ep);

        if (ep.isAcademy()) {
            model.addAttribute("academyReport", reportService.buildAcademyReport(from, to));
            return "angajat-rapoarte-academie";
        } else {
            model.addAttribute("salesReport", reportService.buildSalesReport(from, to, status));
            return "angajat-rapoarte-magazin";
        }
    }

    // ════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════
    private ResponseEntity<ByteArrayResource> fileResponse(byte[] data, String filename, String contentType) {
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(data.length)
                .body(resource);
    }
}
