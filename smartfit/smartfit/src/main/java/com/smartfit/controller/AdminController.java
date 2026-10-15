package com.smartfit.controller;

import com.smartfit.dto.CourseFormDto;
import com.smartfit.dto.EmployeeFormDto;
import com.smartfit.dto.ProductFormDto;
import com.smartfit.entity.*;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Panou de administrare — accesibil EXCLUSIV de ADMIN (vezi SecurityConfig: /admin/** → ROLE_ADMIN).
 *
 * Acoperă toate operațiile cerute:
 *  - adăugare / modificare / ștergere / listare / căutare / filtrare
 *    pentru: produse, cursuri, utilizatori, angajați, comenzi
 *  - rapoarte + grafice (vânzări pe categorie, evoluție zilnică, status comenzi, statistici academie)
 *  - export PDF / Excel, import Excel
 *  - vizualizare log-uri de utilizare aplicație
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductAdminService productAdminService;
    private final CourseAdminService courseAdminService;
    private final EmployeeService employeeService;
    private final ReportService reportService;
    private final ExportService exportService;
    private final ImportService importService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final OrderAdminHelper orderAdminHelper;

    public AdminController(ProductAdminService productAdminService,
                            CourseAdminService courseAdminService,
                            EmployeeService employeeService,
                            ReportService reportService,
                            ExportService exportService,
                            ImportService importService,
                            AuditLogService auditLogService,
                            UserRepository userRepository,
                            OrderAdminHelper orderAdminHelper) {
        this.productAdminService = productAdminService;
        this.courseAdminService = courseAdminService;
        this.employeeService = employeeService;
        this.reportService = reportService;
        this.exportService = exportService;
        this.importService = importService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.orderAdminHelper = orderAdminHelper;
    }

    // ════════════════════════════════════════════════════════════
    // HELPER — obține User curent din SecurityContext
    // ════════════════════════════════════════════════════════════
    private User currentUser(UserDetails principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getUsername());
    }

    // ════════════════════════════════════════════════════════════
    // DASHBOARD
    // ════════════════════════════════════════════════════════════
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("stats", reportService.dashboardStats());
        model.addAttribute("recentLogs", auditLogService.recentLogs());
        model.addAttribute("lowStock", productAdminService.findLowStock(10));
        return "admin-dashboard";
    }

    // ════════════════════════════════════════════════════════════
    // PRODUSE — listare / căutare / filtrare / CRUD
    // ════════════════════════════════════════════════════════════
    @GetMapping("/produse")
    public String products(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) Double minPrice,
                            @RequestParam(required = false) Double maxPrice,
                            @RequestParam(required = false) Boolean inStock,
                            Model model) {
        model.addAttribute("products", productAdminService.search(q, category, minPrice, maxPrice, inStock));
        model.addAttribute("categories", productAdminService.findAllCategories());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("inStock", inStock);
        return "admin-produse";
    }

    @GetMapping("/produse/nou")
    public String newProductForm(Model model) {
        model.addAttribute("product", new ProductFormDto());
        model.addAttribute("categories", productAdminService.findAllCategories());
        model.addAttribute("isNew", true);
        return "admin-produs-form";
    }

    @GetMapping("/produse/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
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
        return "admin-produs-form";
    }

    @PostMapping("/produse/salveaza")
    public String saveProduct(@Valid @ModelAttribute("product") ProductFormDto dto,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal UserDetails principal,
                               HttpServletRequest request,
                               Model model,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", productAdminService.findAllCategories());
            model.addAttribute("isNew", dto.getId() == null);
            return "admin-produs-form";
        }

        try {
            if (dto.getId() == null) {
                productAdminService.create(dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Produs adăugat cu succes!");
            } else {
                productAdminService.update(dto.getId(), dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Produs actualizat cu succes!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/admin/produse";
    }

    @PostMapping("/produse/{id}/sterge")
    public String deleteProduct(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails principal,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {
        try {
            productAdminService.delete(id, currentUser(principal), request);
            ra.addFlashAttribute("success", "Produs șters cu succes!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Nu s-a putut șterge produsul: " + e.getMessage());
        }
        return "redirect:/admin/produse";
    }

    // ── Export / Import produse ─────────────────────────────────
    @GetMapping("/produse/export/excel")
    public ResponseEntity<ByteArrayResource> exportProductsExcel(@AuthenticationPrincipal UserDetails principal,
                                                                   HttpServletRequest request) {
        List<Product> products = productAdminService.findAll();
        byte[] data = exportService.exportProductsExcel(products);

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export Excel produse (" + products.size() + " produse)",
                "products", null, null, request);

        return fileResponse(data, "produse_smartfit.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/produse/export/pdf")
    public ResponseEntity<ByteArrayResource> exportProductsPdf(@AuthenticationPrincipal UserDetails principal,
                                                                 HttpServletRequest request) {
        List<Product> products = productAdminService.findAll();
        List<String> headers = List.of("ID", "Nume", "Categorie", "Preț (lei)", "Stoc");
        List<List<String>> rows = products.stream().map(p -> List.of(
                String.valueOf(p.getId()), p.getName(), p.getCategoryName(),
                String.format("%.2f", p.getPrice()), String.valueOf(p.getStockQuantity())
        )).toList();

        byte[] data = exportService.exportTableToPdf("Catalog produse SmartFit", headers, rows,
                new float[]{0.5f, 3f, 1.5f, 1f, 1f});

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export PDF produse (" + products.size() + " produse)",
                "products", null, null, request);

        return fileResponse(data, "produse_smartfit.pdf", "application/pdf");
    }

    @PostMapping("/produse/import")
    public String importProducts(@RequestParam("file") MultipartFile file,
                                   @AuthenticationPrincipal UserDetails principal,
                                   HttpServletRequest request,
                                   RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Nu a fost selectat niciun fișier.");
            return "redirect:/admin/produse";
        }

        ImportService.ImportResult result = importService.importProducts(file);

        auditLogService.log(currentUser(principal), "IMPORT",
                "Import Excel produse: " + result.imported + " adăugate, " + result.updated
                        + " actualizate, " + result.errors.size() + " erori (din " + result.totalRows + " rânduri)",
                "products", null, null, request);

        if (result.errors.isEmpty()) {
            ra.addFlashAttribute("success", "Import finalizat: " + result.imported
                    + " produse noi, " + result.updated + " actualizate.");
        } else {
            ra.addFlashAttribute("warning", "Import finalizat cu erori: " + result.imported
                    + " adăugate, " + result.updated + " actualizate. Erori: "
                    + String.join(" | ", result.errors));
        }

        return "redirect:/admin/produse";
    }

    // ════════════════════════════════════════════════════════════
    // CURSURI (ACADEMIE) — listare / căutare / filtrare / CRUD
    // ════════════════════════════════════════════════════════════
    @GetMapping("/cursuri")
    public String courses(@RequestParam(required = false) String q,
                           @RequestParam(required = false) String difficulty,
                           @RequestParam(required = false) Integer minPoints,
                           @RequestParam(required = false) Integer maxPoints,
                           Model model) {
        model.addAttribute("courses", courseAdminService.search(q, difficulty, minPoints, maxPoints));
        model.addAttribute("difficulties", courseAdminService.findAllDifficulties());
        model.addAttribute("q", q);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("minPoints", minPoints);
        model.addAttribute("maxPoints", maxPoints);
        return "admin-cursuri";
    }

    @GetMapping("/cursuri/nou")
    public String newCourseForm(Model model) {
        CourseFormDto dto = new CourseFormDto();
        dto.setRewardPoints(50);
        dto.setPassScorePercentage(60);
        dto.setDifficulty("Începător");
        model.addAttribute("course", dto);
        model.addAttribute("isNew", true);
        return "admin-curs-form";
    }

    @GetMapping("/cursuri/{id}/edit")
    public String editCourseForm(@PathVariable Long id, Model model) {
        Course c = courseAdminService.findById(id);
        model.addAttribute("course", courseAdminService.toFormDto(c));
        model.addAttribute("isNew", false);
        return "admin-curs-form";
    }

    @PostMapping("/cursuri/salveaza")
    public String saveCourse(@Valid @ModelAttribute("course") CourseFormDto dto,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal UserDetails principal,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", dto.getId() == null);
            return "admin-curs-form";
        }

        try {
            if (dto.getId() == null) {
                courseAdminService.create(dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Curs adăugat cu succes!");
            } else {
                courseAdminService.update(dto.getId(), dto, currentUser(principal), request);
                ra.addFlashAttribute("success", "Curs actualizat cu succes!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/admin/cursuri";
    }

    @PostMapping("/cursuri/{id}/sterge")
    public String deleteCourse(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails principal,
                                HttpServletRequest request,
                                RedirectAttributes ra) {
        try {
            courseAdminService.delete(id, currentUser(principal), request);
            ra.addFlashAttribute("success", "Curs șters cu succes!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Nu s-a putut șterge cursul: " + e.getMessage());
        }
        return "redirect:/admin/cursuri";
    }

    // ════════════════════════════════════════════════════════════
    // UTILIZATORI — listare / căutare / filtrare (fără CRUD complet,
    // doar vizualizare + ștergere conturi USER)
    // ════════════════════════════════════════════════════════════
    @GetMapping("/utilizatori")
    public String users(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String role,
                         @RequestParam(required = false) String goal,
                         @RequestParam(required = false) String gender,
                         Model model) {
        model.addAttribute("users", userRepository.search(
                blank(q), blank(role), blank(goal), blank(gender)));
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("goal", goal);
        model.addAttribute("gender", gender);
        return "admin-utilizatori";
    }

    @PostMapping("/utilizatori/{id}/sterge")
    public String deleteUser(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails principal,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        User target = userRepository.findById(id).orElse(null);
        if (target == null) {
            ra.addFlashAttribute("error", "Utilizatorul nu a fost găsit.");
            return "redirect:/admin/utilizatori";
        }
        if ("ADMIN".equals(target.getRole())) {
            ra.addFlashAttribute("error", "Nu poți șterge un cont de administrator.");
            return "redirect:/admin/utilizatori";
        }

        String email = target.getEmail();
        userRepository.delete(target);

        auditLogService.log(currentUser(principal), "DELETE",
                "Utilizator șters: " + email,
                "users", id, "DELETE FROM users WHERE id = " + id, request);

        ra.addFlashAttribute("success", "Utilizator șters cu succes!");
        return "redirect:/admin/utilizatori";
    }

    // ════════════════════════════════════════════════════════════
    // ANGAJAȚI — CRUD complet (academie + magazin), permisiuni
    // ════════════════════════════════════════════════════════════
    @GetMapping("/angajati")
    public String employees(@RequestParam(required = false) String department, Model model) {
        if (department != null && !department.isBlank()) {
            model.addAttribute("employees", employeeService.findByDepartment(department));
        } else {
            model.addAttribute("employees", employeeService.findAll());
        }
        model.addAttribute("department", department);
        return "admin-angajati";
    }

    @GetMapping("/angajati/nou")
    public String newEmployeeForm(Model model) {
        EmployeeFormDto dto = new EmployeeFormDto();
        dto.setDepartment("SHOP");
        model.addAttribute("employee", dto);
        model.addAttribute("isNew", true);
        return "admin-angajat-form";
    }

    @GetMapping("/angajati/{id}/edit")
    public String editEmployeeForm(@PathVariable Long id, Model model) {
        EmployeeProfile ep = employeeService.findById(id);
        model.addAttribute("employee", employeeService.toFormDto(ep));
        model.addAttribute("employeeProfileId", id);
        model.addAttribute("isNew", false);
        return "admin-angajat-form";
    }

    @PostMapping("/angajati/nou/salveaza")
    public String createEmployee(@Valid @ModelAttribute("employee") EmployeeFormDto dto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails principal,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin-angajat-form";
        }
        try {
            employeeService.create(dto, currentUser(principal), request);
            ra.addFlashAttribute("success", "Angajat adăugat cu succes!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/admin/angajati";
    }

    @PostMapping("/angajati/{id}/salveaza")
    public String updateEmployee(@PathVariable Long id,
                                  @Valid @ModelAttribute("employee") EmployeeFormDto dto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails principal,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", false);
            model.addAttribute("employeeProfileId", id);
            return "admin-angajat-form";
        }
        try {
            employeeService.update(id, dto, currentUser(principal), request);
            ra.addFlashAttribute("success", "Angajat actualizat cu succes!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/admin/angajati";
    }

    @PostMapping("/angajati/{id}/sterge")
    public String deleteEmployee(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails principal,
                                  HttpServletRequest request,
                                  RedirectAttributes ra) {
        try {
            employeeService.delete(id, currentUser(principal), request);
            ra.addFlashAttribute("success", "Angajat șters cu succes!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/admin/angajati";
    }

    // ════════════════════════════════════════════════════════════
    // COMENZI — listare / căutare / filtrare / schimbare status / export factură
    // ════════════════════════════════════════════════════════════
    @GetMapping("/comenzi")
    public String orders(@RequestParam(required = false) String status,
                          @RequestParam(required = false) String email,
                          @RequestParam(required = false)
                          @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                          LocalDate from,
                          @RequestParam(required = false)
                          @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                          LocalDate to,
                          @RequestParam(required = false) Double minTotal,
                          @RequestParam(required = false) Double maxTotal,
                          Model model) {

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : null;

        model.addAttribute("orders", orderAdminHelper.search(status, email, fromDt, toDt, minTotal, maxTotal));
        model.addAttribute("status", status);
        model.addAttribute("email", email);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("minTotal", minTotal);
        model.addAttribute("maxTotal", maxTotal);
        model.addAttribute("statuses", List.of("PENDING", "SHIPPED", "DELIVERED", "CANCELLED"));
        return "admin-comenzi";
    }

    @PostMapping("/comenzi/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                     @RequestParam String status,
                                     @AuthenticationPrincipal UserDetails principal,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {
        try {
            orderAdminHelper.updateStatus(id, status, currentUser(principal), request);
            ra.addFlashAttribute("success", "Status comandă actualizat: " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Eroare: " + e.getMessage());
        }
        return "redirect:/admin/comenzi";
    }

    @GetMapping("/comenzi/{id}/factura")
    public ResponseEntity<ByteArrayResource> downloadInvoice(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserDetails principal,
                                                               HttpServletRequest request) {
        Order order = orderAdminHelper.getOrder(id);
        byte[] pdf = exportService.exportInvoicePdf(order);

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export factură PDF pentru comanda #ORD-" + id,
                "orders", id, null, request);

        return fileResponse(pdf, "Factura_ORD-" + id + ".pdf", "application/pdf");
    }

    @GetMapping("/comenzi/export/excel")
    public ResponseEntity<ByteArrayResource> exportOrdersExcel(@RequestParam(required = false) String status,
                                                                 @AuthenticationPrincipal UserDetails principal,
                                                                 HttpServletRequest request) {
        List<Order> orders = orderAdminHelper.search(status, null, null, null, null, null);

        List<String> headers = List.of("ID", "Client", "Email", "Total (lei)", "Status", "Data");
        List<List<String>> rows = orders.stream().map(o -> List.of(
                String.valueOf(o.getId()),
                o.getFullName() != null ? o.getFullName() : "-",
                o.getContactEmail() != null ? o.getContactEmail() : "-",
                String.format("%.2f", o.getTotalPrice()),
                o.getStatus(),
                o.getCreatedAt() != null ? o.getCreatedAt().toString() : "-"
        )).toList();

        byte[] data = exportService.exportTableToExcel("Comenzi", headers, rows);

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export Excel comenzi (" + orders.size() + " comenzi)",
                "orders", null, null, request);

        return fileResponse(data, "comenzi_smartfit.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    // ════════════════════════════════════════════════════════════
    // RAPOARTE + GRAFICE
    // ════════════════════════════════════════════════════════════
    @GetMapping("/rapoarte")
    public String reports(@RequestParam(required = false)
                           @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                           LocalDate from,
                           @RequestParam(required = false)
                           @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                           LocalDate to,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false, defaultValue = "SALES") String reportType,
                           Model model) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("status", status);
        model.addAttribute("reportType", reportType);
        model.addAttribute("statuses", List.of("PENDING", "SHIPPED", "DELIVERED", "CANCELLED"));

        if ("ACADEMY".equals(reportType)) {
            model.addAttribute("academyReport", reportService.buildAcademyReport(from, to));
        } else {
            model.addAttribute("salesReport", reportService.buildSalesReport(from, to, status));
        }

        return "admin-rapoarte";
    }

    @GetMapping("/rapoarte/export/pdf")
    public ResponseEntity<ByteArrayResource> exportSalesReportPdf(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        ReportService.SalesReport report = reportService.buildSalesReport(from, to, status);

        List<String> headers = List.of("Comandă", "Client", "Total (lei)", "Status", "Data");
        List<List<String>> rows = report.orders.stream().map(o -> List.of(
                "#ORD-" + o.getId(),
                o.getFullName() != null ? o.getFullName() : "-",
                String.format("%.2f", o.getTotalPrice()),
                o.getStatus(),
                o.getCreatedAt() != null ? o.getCreatedAt().toString() : "-"
        )).toList();

        String title = "Raport vânzări " + from + " → " + to
                + " | Total: " + String.format("%.2f lei", report.totalRevenue)
                + " | Comenzi: " + report.totalOrders;

        byte[] data = exportService.exportTableToPdf(title, headers, rows,
                new float[]{1f, 2f, 1f, 1f, 1.5f});

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export PDF raport vânzări (" + from + " → " + to + ")",
                "orders", null, null, request);

        return fileResponse(data, "raport_vanzari_" + from + "_" + to + ".pdf", "application/pdf");
    }

    @GetMapping("/rapoarte/export/excel")
    public ResponseEntity<ByteArrayResource> exportSalesReportExcel(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        ReportService.SalesReport report = reportService.buildSalesReport(from, to, status);

        List<String> headers = List.of("Comandă", "Client", "Email", "Total (lei)", "Status", "Data");
        List<List<String>> rows = report.orders.stream().map(o -> List.of(
                "ORD-" + o.getId(),
                o.getFullName() != null ? o.getFullName() : "-",
                o.getContactEmail() != null ? o.getContactEmail() : "-",
                String.format("%.2f", o.getTotalPrice()),
                o.getStatus(),
                o.getCreatedAt() != null ? o.getCreatedAt().toString() : "-"
        )).toList();

        byte[] data = exportService.exportTableToExcel("Raport vanzari", headers, rows);

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export Excel raport vânzări (" + from + " → " + to + ")",
                "orders", null, null, request);

        return fileResponse(data, "raport_vanzari_" + from + "_" + to + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    // ════════════════════════════════════════════════════════════
    // LOG-URI DE UTILIZARE APLICAȚIE
    // ════════════════════════════════════════════════════════════
    @GetMapping("/loguri")
    public String logs(@RequestParam(required = false) String email,
                        @RequestParam(required = false) String role,
                        @RequestParam(required = false) String department,
                        @RequestParam(required = false) String actionType,
                        @RequestParam(required = false) String entityType,
                        @RequestParam(required = false)
                        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                        LocalDate from,
                        @RequestParam(required = false)
                        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                        LocalDate to,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        var result = auditLogService.search(email, role, department, actionType, entityType, from, to, page, 30);

        model.addAttribute("logs", result.getContent());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("email", email);
        model.addAttribute("role", role);
        model.addAttribute("department", department);
        model.addAttribute("actionType", actionType);
        model.addAttribute("entityType", entityType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("roles", List.of("USER", "EMPLOYEE", "ADMIN"));
        model.addAttribute("actionTypes", List.of(
                "LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE", "VIEW",
                "EXPORT", "IMPORT", "ORDER_PLACED", "ORDER_STATUS_CHANGE",
                "QUIZ_SUBMIT", "COURSE_ENROLL", "ERROR"));
        model.addAttribute("entityTypes", List.of(
                "products", "courses", "orders", "users", "employee_profiles"));

        return "admin-loguri";
    }

    @GetMapping("/loguri/export/excel")
    public ResponseEntity<ByteArrayResource> exportLogsExcel(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate to,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        List<ApplicationLog> logs = auditLogService.searchAll(email, role, department, actionType, entityType, from, to);

        List<String> headers = List.of("Data/Ora", "Utilizator", "Rol", "Departament", "Acțiune", "Descriere", "Entitate", "ID entitate", "Comandă SQL", "IP");
        List<List<String>> rows = logs.stream().map(l -> List.of(
                l.getLoggedAt() != null ? l.getLoggedAt().toString() : "-",
                l.getUserEmail(),
                l.getUserRole(),
                l.getDepartment() != null ? l.getDepartment() : "-",
                l.getActionType(),
                l.getActionDescription(),
                l.getEntityType() != null ? l.getEntityType() : "-",
                l.getEntityId() != null ? String.valueOf(l.getEntityId()) : "-",
                l.getSqlCommand() != null ? l.getSqlCommand() : "-",
                l.getIpAddress() != null ? l.getIpAddress() : "-"
        )).toList();

        byte[] data = exportService.exportTableToExcel("Log-uri aplicatie", headers, rows);

        auditLogService.log(currentUser(principal), "EXPORT",
                "Export Excel log-uri aplicație (" + logs.size() + " înregistrări)",
                "application_logs", null, null, request);

        return fileResponse(data, "loguri_smartfit.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
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

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
