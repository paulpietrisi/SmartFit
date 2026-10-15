package com.smartfit.service;

import com.smartfit.entity.ApplicationLog;
import com.smartfit.entity.EmployeeProfile;
import com.smartfit.entity.User;
import com.smartfit.repository.ApplicationLogRepository;
import com.smartfit.repository.EmployeeProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Centralizează scrierea log-urilor de aplicație (audit trail).
 * Orice acțiune CRUD din panourile admin/angajat trebuie să apeleze
 * una dintre metodele log(...) de aici.
 */
@Service
public class AuditLogService {

    private final ApplicationLogRepository logRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public AuditLogService(ApplicationLogRepository logRepository,
                            EmployeeProfileRepository employeeProfileRepository) {
        this.logRepository = logRepository;
        this.employeeProfileRepository = employeeProfileRepository;
    }

    /**
     * Înregistrează o acțiune în application_logs.
     *
     * @param user        utilizatorul curent (din SecurityContext)
     * @param actionType  LOGIN/LOGOUT/CREATE/UPDATE/DELETE/VIEW/EXPORT/IMPORT/...
     * @param description text lizibil afișat în UI
     * @param entityType  ex. "products", "courses", "orders", "users" (poate fi null)
     * @param entityId    id-ul rândului afectat (poate fi null)
     * @param sqlCommand  comanda SQL echivalentă, pentru transparență (poate fi null)
     * @param request     folosit pentru IP și User-Agent (poate fi null)
     */
    public void log(User user, String actionType, String description,
                     String entityType, Long entityId, String sqlCommand,
                     HttpServletRequest request) {

        ApplicationLog log = new ApplicationLog();

        if (user != null) {
            log.setUserId(user.getId());
            log.setUserEmail(user.getEmail());
            log.setUserRole(user.getRole());

            if ("EMPLOYEE".equals(user.getRole())) {
                employeeProfileRepository.findByUserId(user.getId())
                        .ifPresent(ep -> log.setDepartment(ep.getDepartment()));
            }
        } else {
            log.setUserEmail("anonim");
            log.setUserRole("ANONIM");
        }

        log.setActionType(actionType);
        log.setActionDescription(description);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setSqlCommand(sqlCommand);

        if (request != null) {
            log.setIpAddress(extractIp(request));
            String ua = request.getHeader("User-Agent");
            if (ua != null && ua.length() > 500) ua = ua.substring(0, 500);
            log.setUserAgent(ua);
        }

        logRepository.save(log);
    }

    /** Variantă simplificată, fără request (ex. apeluri din servicii batch). */
    public void log(User user, String actionType, String description,
                     String entityType, Long entityId, String sqlCommand) {
        log(user, actionType, description, entityType, entityId, sqlCommand, null);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ════════════════════════════════════════════════════════════
    // CITIRE / FILTRARE (folosit de AdminController pentru pag. Log-uri)
    // ════════════════════════════════════════════════════════════

    public Page<ApplicationLog> search(String email, String role, String department,
                                        String actionType, String entityType,
                                        LocalDate from, LocalDate to,
                                        int page, int size) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("loggedAt").descending());

        return logRepository.search(
                blankToNull(email), blankToNull(role), blankToNull(department),
                blankToNull(actionType), blankToNull(entityType),
                fromDt, toDt, pageable);
    }

    public List<ApplicationLog> searchAll(String email, String role, String department,
                                           String actionType, String entityType,
                                           LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        return logRepository.searchAll(
                blankToNull(email), blankToNull(role), blankToNull(department),
                blankToNull(actionType), blankToNull(entityType),
                fromDt, toDt);
    }

    public List<ApplicationLog> recentLogs() {
        return logRepository.findTop50ByOrderByLoggedAtDesc();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
