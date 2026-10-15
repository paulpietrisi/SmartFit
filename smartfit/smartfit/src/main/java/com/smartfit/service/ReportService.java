package com.smartfit.service;

import com.smartfit.entity.Order;
import com.smartfit.repository.CourseCompletionRepository;
import com.smartfit.repository.CourseEnrollmentRepository;
import com.smartfit.repository.OrderRepository;
import com.smartfit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregă date din mai multe tabele pentru pagina de Rapoarte:
 * - vânzări pe perioadă
 * - vânzări pe categorie (pentru grafic)
 * - evoluție zilnică (pentru grafic linie)
 * - status comenzi (pentru grafic pie)
 * - statistici cursuri
 */
@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CourseCompletionRepository courseCompletionRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    public ReportService(OrderRepository orderRepository,
                          UserRepository userRepository,
                          CourseCompletionRepository courseCompletionRepository,
                          CourseEnrollmentRepository courseEnrollmentRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.courseCompletionRepository = courseCompletionRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    /** Container simplu pentru rezultatul agregat al raportului de vânzări. */
    public static class SalesReport {
        public double totalRevenue;
        public long totalOrders;
        public List<Order> orders = new ArrayList<>();

        // Date pentru grafice (chart_display_v0 / Chart.js)
        public List<String> categoryLabels = new ArrayList<>();
        public List<Double> categoryValues = new ArrayList<>();

        public List<String> dailyLabels = new ArrayList<>();
        public List<Double> dailyValues = new ArrayList<>();
        public List<Long> dailyOrderCounts = new ArrayList<>();

        public Map<String, Long> statusDistribution = new LinkedHashMap<>();

        public List<Object[]> topProducts = new ArrayList<>();
    }

    /**
     * Construiește raportul complet de vânzări pentru perioada [from, to].
     * Folosit de pagina rapoarte.html (admin & angajat magazin).
     */
    public SalesReport buildSalesReport(LocalDate from, LocalDate to, String statusFilter) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        SalesReport report = new SalesReport();

        // Comenzi (cu filtru opțional de status)
        report.orders = orderRepository.search(
                blank(statusFilter), null, fromDt, toDt, null, null);

        report.totalOrders = report.orders.size();
        report.totalRevenue = report.orders.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();

        // Vânzări pe categorie
        for (Object[] row : orderRepository.salesByCategory(fromDt, toDt)) {
            report.categoryLabels.add((String) row[0]);
            report.categoryValues.add(((Number) row[1]).doubleValue());
        }

        // Evoluție zilnică
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
        for (Object[] row : orderRepository.dailySales(fromDt, toDt)) {
            java.sql.Date d = (java.sql.Date) row[0];
            report.dailyLabels.add(d.toLocalDate().format(fmt));
            report.dailyValues.add(((Number) row[1]).doubleValue());
            report.dailyOrderCounts.add(((Number) row[2]).longValue());
        }

        // Distribuție status
        for (Object[] row : orderRepository.statusDistribution(fromDt, toDt)) {
            report.statusDistribution.put((String) row[0], ((Number) row[1]).longValue());
        }

        // Top produse
        report.topProducts = orderRepository.topProducts(fromDt, toDt);

        return report;
    }

    /** Container pentru raportul Academie. */
    public static class AcademyReport {
        public long totalCompletions;
        public double averageScore;
        public List<Object[]> statsByCourse = new ArrayList<>(); // [title, count, avgScore]
        public List<Object[]> enrollmentCounts = new ArrayList<>(); // [title, count]
    }

    public AcademyReport buildAcademyReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        AcademyReport report = new AcademyReport();
        report.statsByCourse = courseCompletionRepository.completionStatsByCourse(fromDt, toDt);
        report.enrollmentCounts = courseEnrollmentRepository.enrollmentCountPerCourse();

        long total = 0;
        double sumAvg = 0;
        int n = 0;
        for (Object[] row : report.statsByCourse) {
            total += ((Number) row[1]).longValue();
            if (row[2] != null) {
                sumAvg += ((Number) row[2]).doubleValue();
                n++;
            }
        }
        report.totalCompletions = total;
        report.averageScore = n > 0 ? sumAvg / n : 0;

        return report;
    }

    /** Statistici generale pentru dashboard admin. */
    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalEmployees", userRepository.findByRole("EMPLOYEE").size());
        stats.put("monthlyRevenue", orderRepository.sumRevenueBetween(monthStart, now));
        stats.put("monthlyOrders", orderRepository.countBetween(monthStart, now));

        return stats;
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
