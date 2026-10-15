package com.smartfit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfit.entity.*;
import com.smartfit.repository.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Centralizează generarea exporturilor reale din pagina /setari:
 * Jurnal alimentar (PDF+CSV), Comenzi & Facturi (PDF), Academie & XP (PDF+CSV),
 * Greutate & IMC (CSV+JSON), Profil & Setări (JSON) și Date complete (ZIP).
 */
@Service
public class SettingsExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final DiaryEntryRepository diaryEntryRepository;
    private final WeightHistoryRepository weightHistoryRepository;
    private final OrderRepository orderRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseCompletionRepository courseCompletionRepository;
    private final ExportService exportService;

    public SettingsExportService(DiaryEntryRepository diaryEntryRepository,
                                 WeightHistoryRepository weightHistoryRepository,
                                 OrderRepository orderRepository,
                                 CourseEnrollmentRepository courseEnrollmentRepository,
                                 CourseCompletionRepository courseCompletionRepository,
                                 ExportService exportService) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.weightHistoryRepository = weightHistoryRepository;
        this.orderRepository = orderRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.courseCompletionRepository = courseCompletionRepository;
        this.exportService = exportService;
    }

    // ════════════════════════════════════════════════════════════
    // 1. PROFIL & SETĂRI — JSON
    // ════════════════════════════════════════════════════════════

    public byte[] profilSetariJson(User user, UserSettings settings) {
        Map<String, Object> profil = new LinkedHashMap<>();
        profil.put("id", user.getId());
        profil.put("email", user.getEmail());
        profil.put("firstName", user.getFirstName());
        profil.put("lastName", user.getLastName());
        profil.put("role", user.getRole());
        profil.put("gender", user.getGender());
        profil.put("birthDate", user.getBirthDate());
        profil.put("heightCm", user.getHeightCm());
        profil.put("weightKg", user.getWeightKg());
        profil.put("goal", user.getGoal());
        profil.put("activityLevel", user.getActivityLevel());
        profil.put("fitPoints", user.getFitPoints());

        Map<String, Object> setariMap = new LinkedHashMap<>();
        setariMap.put("language", settings.getLanguage());
        setariMap.put("unitSystem", settings.getUnitSystem());
        setariMap.put("timezone", settings.getTimezone());
        setariMap.put("weekStart", settings.getWeekStart());
        setariMap.put("dateFormat", settings.getDateFormat());
        setariMap.put("dailyCalories", settings.getDailyCalories());
        setariMap.put("dailyProtein", settings.getDailyProtein());
        setariMap.put("dailyCarbs", settings.getDailyCarbs());
        setariMap.put("dailyFat", settings.getDailyFat());
        setariMap.put("dailyFiber", settings.getDailyFiber());
        setariMap.put("dailyWater", settings.getDailyWater());
        setariMap.put("theme", settings.getTheme());
        setariMap.put("accentColor", settings.getAccentColor());
        setariMap.put("fontSize", settings.getFontSize());
        setariMap.put("profileVisibility", settings.getProfileVisibility());
        setariMap.put("leaderboardVisible", settings.isLeaderboardVisible());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportatLa", java.time.LocalDateTime.now());
        data.put("profil", profil);
        data.put("setari", setariMap);

        return toJsonBytes(data);
    }

    // ════════════════════════════════════════════════════════════
    // 2. JURNAL ALIMENTAR — CSV + PDF
    // ════════════════════════════════════════════════════════════

    private static final List<String> JURNAL_HEADERS = List.of(
            "Data", "Masă", "Aliment", "Cantitate (g)", "Calorii (kcal)",
            "Proteine (g)", "Carbohidrați (g)", "Grăsimi (g)");

    private List<List<String>> jurnalRows(User user) {
        List<DiaryEntry> entries = diaryEntryRepository.findByUserWithFoodOrderByDateDesc(user);
        List<List<String>> rows = new ArrayList<>();
        for (DiaryEntry e : entries) {
            Food f = e.getFood();
            double qty = e.getQuantityG() != null ? e.getQuantityG() : 0;
            double factor = qty / 100.0;
            double kcal = (f != null && f.getKcalPer100g() != null) ? f.getKcalPer100g() * factor : 0;
            double prot = (f != null && f.getProtein() != null) ? f.getProtein() * factor : 0;
            double carb = (f != null && f.getCarbs() != null) ? f.getCarbs() * factor : 0;
            double fat  = (f != null && f.getFat() != null) ? f.getFat() * factor : 0;

            rows.add(List.of(
                    e.getConsumedDate() != null ? e.getConsumedDate().format(DATE_FMT) : "-",
                    mealLabel(e.getMealType()),
                    f != null ? f.getName() : "-",
                    String.format("%.0f", qty),
                    String.format("%.0f", kcal),
                    String.format("%.1f", prot),
                    String.format("%.1f", carb),
                    String.format("%.1f", fat)
            ));
        }
        return rows;
    }

    public byte[] jurnalAlimentarCsv(User user) {
        return toCsvBytes(JURNAL_HEADERS, jurnalRows(user));
    }

    public byte[] jurnalAlimentarPdf(User user) {
        return exportService.exportTableToPdf(
                "Jurnal alimentar — " + fullName(user),
                JURNAL_HEADERS, jurnalRows(user),
                new float[]{1.2f, 1f, 2f, 1f, 1f, 1f, 1f, 1f});
    }

    private String mealLabel(String mealType) {
        if (mealType == null) return "-";
        return switch (mealType.toUpperCase()) {
            case "BREAKFAST" -> "Mic dejun";
            case "LUNCH" -> "Prânz";
            case "DINNER" -> "Cină";
            case "SNACK" -> "Gustare";
            default -> mealType;
        };
    }

    // ════════════════════════════════════════════════════════════
    // 3. COMENZI & FACTURI — PDF
    // ════════════════════════════════════════════════════════════

    private static final List<String> COMENZI_HEADERS = List.of(
            "Data", "Comandă #", "Status", "Plată", "Produse", "Total (lei)");

    public byte[] comenziPdf(User user) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<List<String>> rows = new ArrayList<>();
        for (Order o : orders) {
            String produse = o.getItems().stream()
                    .map(i -> i.getProduct().getName() + " ×" + i.getQuantity())
                    .collect(Collectors.joining("; "));
            rows.add(List.of(
                    o.getCreatedAt() != null ? o.getCreatedAt().format(DATETIME_FMT) : "-",
                    "#ORD-" + o.getId(),
                    statusLabel(o.getStatus()),
                    "CARD".equalsIgnoreCase(o.getPaymentMethod()) ? "Card online" : "Ramburs",
                    produse.isEmpty() ? "-" : produse,
                    o.getTotalPrice() != null ? String.format("%.2f", o.getTotalPrice()) : "0.00"
            ));
        }
        return exportService.exportTableToPdf(
                "Istoric comenzi — " + fullName(user),
                COMENZI_HEADERS, rows,
                new float[]{1.3f, 1f, 1f, 1f, 3f, 1f});
    }

    private String statusLabel(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "În așteptare";
            case "PROCESSING" -> "În procesare";
            case "SHIPPED" -> "Expediată";
            case "DELIVERED" -> "Livrată";
            case "CANCELLED" -> "Anulată";
            default -> status;
        };
    }

    // ════════════════════════════════════════════════════════════
    // 4. ACADEMIE & XP — CSV + PDF
    // ════════════════════════════════════════════════════════════

    private static final List<String> ACADEMIE_HEADERS = List.of(
            "Curs", "Data înscriere", "Progres", "Finalizat", "Scor", "Data finalizare", "XP câștigat");

    private List<List<String>> academieRows(User user) {
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByUserId(user.getId());
        List<CourseCompletion> completions = courseCompletionRepository.findByUserId(user.getId());

        Map<Long, CourseCompletion> completionByCourseId = completions.stream()
                .filter(c -> c.getCourse() != null)
                .collect(Collectors.toMap(c -> c.getCourse().getId(), c -> c, (a, b) -> a));

        List<List<String>> rows = new ArrayList<>();
        for (CourseEnrollment e : enrollments) {
            Course c = e.getCourse();
            if (c == null) continue;
            CourseCompletion comp = completionByCourseId.get(c.getId());
            boolean finalized = comp != null;
            int xp = finalized && c.getRewardPoints() != null ? c.getRewardPoints() : 0;

            rows.add(List.of(
                    c.getTitle() != null ? c.getTitle() : "-",
                    e.getEnrolledAt() != null ? e.getEnrolledAt().format(DATETIME_FMT) : "-",
                    (e.getProgressPct() != null ? e.getProgressPct() : 0) + "%",
                    finalized ? "Da" : "Nu",
                    finalized && comp.getScoreObtained() != null ? comp.getScoreObtained() + "%" : "-",
                    finalized && comp.getCompletedAt() != null ? comp.getCompletedAt().format(DATETIME_FMT) : "-",
                    String.valueOf(xp)
            ));
        }
        return rows;
    }

    public byte[] academieCsv(User user) {
        return toCsvBytes(ACADEMIE_HEADERS, academieRows(user));
    }

    public byte[] academiePdf(User user) {
        return exportService.exportTableToPdf(
                "Academie & XP — " + fullName(user),
                ACADEMIE_HEADERS, academieRows(user),
                new float[]{2.5f, 1.3f, 1f, 1f, 1f, 1.3f, 1f});
    }

    // ════════════════════════════════════════════════════════════
    // 5. GREUTATE & IMC — CSV + JSON
    // ════════════════════════════════════════════════════════════

    private static final List<String> GREUTATE_HEADERS = List.of("Data", "Greutate (kg)", "IMC");

    private List<WeightHistory> sortedWeightHistory(User user) {
        List<WeightHistory> list = new ArrayList<>(weightHistoryRepository.findByUser(user));
        list.sort(Comparator.comparing(WeightHistory::getLoggedDate));
        return list;
    }

    private Double computeBmi(Double weightKg, Double heightCm) {
        if (weightKg == null || heightCm == null || heightCm <= 0) return null;
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    public byte[] greutateCsv(User user) {
        Double heightCm = user.getHeightCm();
        List<WeightHistory> list = sortedWeightHistory(user);
        List<List<String>> rows = new ArrayList<>();
        for (WeightHistory w : list) {
            Double bmi = computeBmi(w.getWeightKg(), heightCm);
            rows.add(List.of(
                    w.getLoggedDate() != null ? w.getLoggedDate().format(DATE_FMT) : "-",
                    w.getWeightKg() != null ? String.format("%.1f", w.getWeightKg()) : "-",
                    bmi != null ? String.format("%.1f", bmi) : "-"
            ));
        }
        return toCsvBytes(GREUTATE_HEADERS, rows);
    }

    public byte[] greutateJson(User user) {
        Double heightCm = user.getHeightCm();
        List<WeightHistory> list = sortedWeightHistory(user);

        List<Map<String, Object>> entries = new ArrayList<>();
        for (WeightHistory w : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("data", w.getLoggedDate());
            m.put("greutateKg", w.getWeightKg());
            m.put("imc", computeBmi(w.getWeightKg(), heightCm));
            entries.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportatLa", java.time.LocalDateTime.now());
        data.put("inaltimeCm", heightCm);
        data.put("inregistrari", entries);

        return toJsonBytes(data);
    }

    // ════════════════════════════════════════════════════════════
    // 6. DATE COMPLETE — ZIP (bundle peste toate exporturile)
    // ════════════════════════════════════════════════════════════

    public byte[] dateCompleteZip(User user, UserSettings settings) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            addZipEntry(zos, "profil-si-setari.json", profilSetariJson(user, settings));
            addZipEntry(zos, "jurnal-alimentar.csv", jurnalAlimentarCsv(user));
            addZipEntry(zos, "jurnal-alimentar.pdf", jurnalAlimentarPdf(user));
            addZipEntry(zos, "comenzi-si-facturi.pdf", comenziPdf(user));
            addZipEntry(zos, "academie-si-xp.csv", academieCsv(user));
            addZipEntry(zos, "academie-si-xp.pdf", academiePdf(user));
            addZipEntry(zos, "greutate-si-imc.csv", greutateCsv(user));
            addZipEntry(zos, "greutate-si-imc.json", greutateJson(user));

            String readme = "Export complet SmartFit\n"
                    + "Cont: " + user.getEmail() + "\n"
                    + "Generat la: " + java.time.LocalDateTime.now().format(DATETIME_FMT) + "\n\n"
                    + "Acest arhivă conține toate datele tale personale din SmartFit, "
                    + "conform dreptului de acces si portabilitate GDPR.";
            addZipEntry(zos, "CITESTE-MA.txt", readme.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zos, String name, byte[] content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content);
        zos.closeEntry();
    }

    // ════════════════════════════════════════════════════════════
    // UTILITARE COMUNE
    // ════════════════════════════════════════════════════════════

    private String fullName(User user) {
        String fn = user.getFirstName() != null ? user.getFirstName() : "";
        String ln = user.getLastName() != null ? user.getLastName() : "";
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }

    /** Construiește un CSV (cu BOM UTF-8, separator virgulă, escaping corect) ca byte[]. */
    private byte[] toCsvBytes(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(headers.stream().map(this::csvField).collect(Collectors.joining(","))).append("\n");
        for (List<String> row : rows) {
            sb.append(row.stream().map(this::csvField).collect(Collectors.joining(","))).append("\n");
        }
        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[UTF8_BOM.length + csvBytes.length];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        System.arraycopy(csvBytes, 0, withBom, UTF8_BOM.length, csvBytes.length);
        return withBom;
    }

    private String csvField(String value) {
        if (value == null) value = "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Serializează un obiect ca JSON formatat, cu suport pentru LocalDate/LocalDateTime. */
    private byte[] toJsonBytes(Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Eroare la generarea JSON: " + e.getMessage(), e);
        }
    }
}