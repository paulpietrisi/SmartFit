package com.smartfit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfit.entity.ApplicationLog;
import com.smartfit.entity.User;
import com.smartfit.entity.UserSettings;
import com.smartfit.repository.DailyLogRepository;
import com.smartfit.repository.DiaryEntryRepository;
import com.smartfit.repository.UserRepository;
import com.smartfit.repository.UserSettingsRepository;
import com.smartfit.repository.WeightHistoryRepository;
import com.smartfit.service.AuditLogService;
import com.smartfit.service.SettingsExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Controller
public class SettingsController {

    @Autowired private UserRepository userRepository;
    @Autowired private UserSettingsRepository userSettingsRepository;
    @Autowired private DailyLogRepository dailyLogRepository;
    @Autowired private DiaryEntryRepository diaryEntryRepository;
    @Autowired private WeightHistoryRepository weightHistoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditLogService auditLogService;
    @Autowired private SettingsExportService settingsExportService;

    // ── Utilitare ──
    private User getCurrentUser(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName());
        if (user == null) throw new RuntimeException("User negasit: " + auth.getName());
        return user;
    }

    private UserSettings getOrCreate(User user) {
        return userSettingsRepository.findByUser(user).orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setUser(user);
            return userSettingsRepository.save(s);
        });
    }

    /** Ultimele 10 evenimente de securitate/setări ale userului curent, cele mai recente primele. */
    private List<ApplicationLog> recentAuditLogs(User user) {
        return auditLogService.searchAll(user.getEmail(), null, null, null, null, null, null)
                .stream()
                .sorted(Comparator.comparing(ApplicationLog::getLoggedAt).reversed())
                .limit(10)
                .toList();
    }

    // ════════════════════════════════════════
    // GET /setari
    // ════════════════════════════════════════
    @GetMapping("/setari")
    public String showSettings(Model model, Authentication auth,
                               @RequestParam(defaultValue = "general") String panel) {
        User user = getCurrentUser(auth);
        UserSettings settings = getOrCreate(user);
        model.addAttribute("settings", settings);
        model.addAttribute("user", user);
        model.addAttribute("activePanel", panel);
        model.addAttribute("auditLogs", recentAuditLogs(user));
        return "setari";
    }

    // ════════════════════════════════════════
    // POST /setari — salveaza toate setarile
    // ════════════════════════════════════════
    @PostMapping("/setari")
    public String saveSettings(@ModelAttribute("settings") UserSettings form,
                               Authentication auth,
                               HttpServletRequest request,
                               RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        UserSettings s = getOrCreate(user);

        // General
        s.setLanguage(form.getLanguage());
        s.setUnitSystem(form.getUnitSystem());
        s.setTimezone(form.getTimezone());
        s.setWeekStart(form.getWeekStart());
        s.setDateFormat(form.getDateFormat());

        // Tracking
        s.setDailyReminderTime(form.getDailyReminderTime());
        s.setDailyReminderEnabled(form.isDailyReminderEnabled());
        s.setWeightReminderTime(form.getWeightReminderTime());
        s.setWeightReminderEnabled(form.isWeightReminderEnabled());
        s.setHydrationInterval(form.getHydrationInterval());
        s.setHydrationEnabled(form.isHydrationEnabled());
        s.setDefaultMacros(form.getDefaultMacros());
        s.setStreakNotification(form.isStreakNotification());

        // Obiective
        s.setDailyCalories(form.getDailyCalories());
        s.setDailyProtein(form.getDailyProtein());
        s.setDailyCarbs(form.getDailyCarbs());
        s.setDailyFat(form.getDailyFat());
        s.setDailyFiber(form.getDailyFiber());
        s.setDailyWater(form.getDailyWater());

        // Aspect
        s.setTheme(form.getTheme());
        s.setAccentColor(form.getAccentColor());
        s.setFontSize(form.getFontSize());
        s.setAnimationsEnabled(form.isAnimationsEnabled());
        s.setBlurEffect(form.isBlurEffect());
        s.setAnimatedCharts(form.isAnimatedCharts());
        s.setCompactMode(form.isCompactMode());
        s.setTooltipsEnabled(form.isTooltipsEnabled());

        // Notificari – canale
        s.setEmailNotifications(form.isEmailNotifications());
        s.setPushNotifications(form.isPushNotifications());
        s.setSmsNotifications(form.isSmsNotifications());

        // Notificari – tipuri
        s.setNotifDailyJournal(form.isNotifDailyJournal());
        s.setNotifCalorieGoal(form.isNotifCalorieGoal());
        s.setNotifCalorieExceed(form.isNotifCalorieExceed());
        s.setNotifHydration(form.isNotifHydration());
        s.setNotifOrderConfirm(form.isNotifOrderConfirm());
        s.setNotifDelivery(form.isNotifDelivery());
        s.setNotifFavoriteBack(form.isNotifFavoriteBack());
        s.setNotifOffers(form.isNotifOffers());
        s.setNotifNewCourse(form.isNotifNewCourse());
        s.setNotifStreakWarning(form.isNotifStreakWarning());
        s.setNotifNewLevel(form.isNotifNewLevel());
        s.setNotifAcademySummary(form.isNotifAcademySummary());
        s.setNotifNutritionSummary(form.isNotifNutritionSummary());
        s.setNotifReportReady(form.isNotifReportReady());
        s.setNotifNewLogin(form.isNotifNewLogin());

        // Program silentios
        s.setQuietMode(form.isQuietMode());
        s.setQuietFrom(form.getQuietFrom());
        s.setQuietTo(form.getQuietTo());

        // Confidentialitate
        s.setProfileVisibility(form.getProfileVisibility());
        s.setLeaderboardVisible(form.isLeaderboardVisible());
        s.setShareStats(form.isShareStats());
        s.setUsageAnalytics(form.isUsageAnalytics());
        s.setPersonalizationCookies(form.isPersonalizationCookies());
        s.setPersonalizedRecommendations(form.isPersonalizedRecommendations());
        s.setAiImprovement(form.isAiImprovement());

        // Securitate & Date
        s.setSessionTimeout(form.getSessionTimeout());
        s.setJournalRetention(form.getJournalRetention());
        s.setOrdersRetention(form.getOrdersRetention());
        s.setReportsRetention(form.getReportsRetention());

        userSettingsRepository.save(s);
        auditLogService.log(user, "UPDATE", "Setări actualizate", "user_settings", s.getId(), null, request);

        ra.addFlashAttribute("settingsSaved", true);
        return "redirect:/setari";
    }

    // ════════════════════════════════════════
    // POST /setari/schimba-parola
    // ════════════════════════════════════════
    @PostMapping("/setari/schimba-parola")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {
        User user = getCurrentUser(auth);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            auditLogService.log(user, "ERROR", "Tentativă eșuată de schimbare parolă (parolă actuală incorectă)", "user", user.getId(), null, request);
            ra.addFlashAttribute("pwError", "Parola actuală este incorectă.");
            ra.addFlashAttribute("openPwModal", true);
            return "redirect:/setari?panel=security";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("pwError", "Parola nouă trebuie să aibă minim 8 caractere.");
            ra.addFlashAttribute("openPwModal", true);
            return "redirect:/setari?panel=security";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("pwError", "Parolele noi nu coincid.");
            ra.addFlashAttribute("openPwModal", true);
            return "redirect:/setari?panel=security";
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log(user, "UPDATE", "Parolă schimbată cu succes", "user", user.getId(), null, request);

        ra.addFlashAttribute("settingsSaved", true);
        return "redirect:/setari?panel=security";
    }

    // ════════════════════════════════════════
    // POST /setari/schimba-email
    // ════════════════════════════════════════
    @PostMapping("/setari/schimba-email")
    public String changeEmail(@RequestParam String newEmail,
                              @RequestParam String confirmPassword,
                              Authentication auth,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        User user = getCurrentUser(auth);

        if (!passwordEncoder.matches(confirmPassword, user.getPasswordHash())) {
            auditLogService.log(user, "ERROR", "Tentativă eșuată de schimbare email (parolă incorectă)", "user", user.getId(), null, request);
            ra.addFlashAttribute("emailError", "Parola este incorectă.");
            ra.addFlashAttribute("openEmailModal", true);
            return "redirect:/setari?panel=account";
        }
        if (userRepository.findByEmail(newEmail) != null) {
            ra.addFlashAttribute("emailError", "Adresa de email este deja folosită.");
            ra.addFlashAttribute("openEmailModal", true);
            return "redirect:/setari?panel=account";
        }

        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        userRepository.save(user);
        auditLogService.log(user, "UPDATE", "Email schimbat din " + oldEmail + " în " + newEmail, "user", user.getId(), null, request);

        // Sesiunea curentă e legată de vechiul email (Spring Security username) — forțăm relogin
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        return "redirect:/login?emailChanged";
    }

    // ════════════════════════════════════════
    // POST /setari/reseteaza — resetare setari la default
    // ════════════════════════════════════════
    @PostMapping("/setari/reseteaza")
    public String resetSettings(Authentication auth, HttpServletRequest request, RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        UserSettings existing = getOrCreate(user);

        UserSettings defaults = new UserSettings();
        defaults.setId(existing.getId());
        defaults.setUser(user);
        userSettingsRepository.save(defaults);
        auditLogService.log(user, "UPDATE", "Setări resetate la valorile implicite", "user_settings", existing.getId(), null, request);

        ra.addFlashAttribute("settingsReset", true);
        return "redirect:/setari";
    }

    // ════════════════════════════════════════
    // POST /setari/sterge-istoric
    // ════════════════════════════════════════
    @PostMapping("/setari/sterge-istoric")
    @org.springframework.transaction.annotation.Transactional
    public String deleteHistory(Authentication auth, HttpServletRequest request, RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        int diaryCount = diaryEntryRepository.findByUser(user).size();
        diaryEntryRepository.deleteAll(diaryEntryRepository.findByUser(user));
        dailyLogRepository.deleteAll(dailyLogRepository.findByUser(user));
        weightHistoryRepository.deleteAll(weightHistoryRepository.findByUser(user));

        auditLogService.log(user, "DELETE", "Istoric alimentar și greutate șters integral (" + diaryCount + " înregistrări jurnal)",
                "diary_entries", null, null, request);

        ra.addFlashAttribute("settingsSaved", true);
        return "redirect:/setari?panel=account";
    }

    // ════════════════════════════════════════
    // POST /setari/dezactiveaza
    // ════════════════════════════════════════
    @PostMapping("/setari/dezactiveaza")
    public String deactivateAccount(@RequestParam String password,
                                    Authentication auth,
                                    HttpServletRequest request,
                                    RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLogService.log(user, "ERROR", "Tentativă eșuată de dezactivare cont (parolă incorectă)", "user", user.getId(), null, request);
            ra.addFlashAttribute("deactivateError", "Parola incorectă.");
            return "redirect:/setari?panel=account";
        }
        // TODO: necesita camp `active`/`enabled` pe entitatea User pentru dezactivare reala:
        // user.setActive(false);
        // userRepository.save(user);
        auditLogService.log(user, "UPDATE", "Cont dezactivat de utilizator", "user", user.getId(), null, request);

        ra.addFlashAttribute("settingsSaved", true);
        return "redirect:/setari?panel=account";
    }

    // ════════════════════════════════════════
    // POST /setari/sterge-cont
    // ════════════════════════════════════════
    @PostMapping("/setari/sterge-cont")
    @org.springframework.transaction.annotation.Transactional
    public String deleteAccount(@RequestParam String deleteConfirm,
                                @RequestParam String password,
                                Authentication auth,
                                HttpServletRequest request,
                                RedirectAttributes ra) {
        User user = getCurrentUser(auth);

        if (!"ȘTERGE CONTUL".equals(deleteConfirm)) {
            ra.addFlashAttribute("deleteError", "Textul de confirmare este incorect.");
            ra.addFlashAttribute("openDeleteModal", true);
            return "redirect:/setari?panel=account";
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLogService.log(user, "ERROR", "Tentativă eșuată de ștergere cont (parolă incorectă)", "user", user.getId(), null, request);
            ra.addFlashAttribute("deleteError", "Parola incorectă.");
            ra.addFlashAttribute("openDeleteModal", true);
            return "redirect:/setari?panel=account";
        }

        // Logam ÎNAINTE de stergere (avem nevoie de userId/email înca valide)
        auditLogService.log(user, "DELETE", "Cont șters definitiv de utilizator", "user", user.getId(), null, request);

        // Sterge toate datele utilizatorului
        diaryEntryRepository.deleteAll(diaryEntryRepository.findByUser(user));
        dailyLogRepository.deleteAll(dailyLogRepository.findByUser(user));
        weightHistoryRepository.deleteAll(weightHistoryRepository.findByUser(user));
        userSettingsRepository.findByUser(user).ifPresent(userSettingsRepository::delete);
        userRepository.delete(user);

        // Invalideaza sesiunea
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();

        return "redirect:/login?accountDeleted";
    }

    // ════════════════════════════════════════════════════════════
    // EXPORTURI REALE — toate scriu fisierul direct in response
    // ════════════════════════════════════════════════════════════

    private void writeDownload(HttpServletResponse response, byte[] content,
                               String contentType, String filename) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
        response.getOutputStream().flush();
    }

    @GetMapping("/setari/export/profil")
    public void exportProfileJson(Authentication auth, HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        UserSettings settings = getOrCreate(user);
        byte[] json = settingsExportService.profilSetariJson(user, settings);
        auditLogService.log(user, "EXPORT", "Export profil și setări (JSON)", "user", user.getId(), null, request);
        writeDownload(response, json, "application/json; charset=UTF-8", "smartfit-profil-setari.json");
    }

    @GetMapping("/setari/export/jurnal.csv")
    public void exportJurnalCsv(Authentication auth, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        byte[] csv = settingsExportService.jurnalAlimentarCsv(user);
        auditLogService.log(user, "EXPORT", "Export jurnal alimentar (CSV)", "diary_entries", null, null, request);
        writeDownload(response, csv, "text/csv; charset=UTF-8", "smartfit-jurnal-alimentar.csv");
    }

    @GetMapping("/setari/export/jurnal.pdf")
    public void exportJurnalPdf(Authentication auth, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        byte[] pdf = settingsExportService.jurnalAlimentarPdf(user);
        auditLogService.log(user, "EXPORT", "Export jurnal alimentar (PDF)", "diary_entries", null, null, request);
        writeDownload(response, pdf, "application/pdf", "smartfit-jurnal-alimentar.pdf");
    }

    @GetMapping("/setari/export/comenzi.pdf")
    public void exportComenziPdf(Authentication auth, HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        byte[] pdf = settingsExportService.comenziPdf(user);
        auditLogService.log(user, "EXPORT", "Export istoric comenzi (PDF)", "orders", null, null, request);
        writeDownload(response, pdf, "application/pdf", "smartfit-comenzi-facturi.pdf");
    }

    @GetMapping("/setari/export/academie.csv")
    public void exportAcademieCsv(Authentication auth, HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        byte[] csv = settingsExportService.academieCsv(user);
        auditLogService.log(user, "EXPORT", "Export Academie & XP (CSV)", "course_enrollments", null, null, request);
        writeDownload(response, csv, "text/csv; charset=UTF-8", "smartfit-academie-xp.csv");
    }

    @GetMapping("/setari/export/academie.pdf")
    public void exportAcademiePdf(Authentication auth, HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        byte[] pdf = settingsExportService.academiePdf(user);
        auditLogService.log(user, "EXPORT", "Export Academie & XP (PDF)", "course_enrollments", null, null, request);
        writeDownload(response, pdf, "application/pdf", "smartfit-academie-xp.pdf");
    }

    @GetMapping("/setari/export/greutate.csv")
    public void exportGreutateCsv(Authentication auth, HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        byte[] csv = settingsExportService.greutateCsv(user);
        auditLogService.log(user, "EXPORT", "Export greutate & IMC (CSV)", "weight_history", null, null, request);
        writeDownload(response, csv, "text/csv; charset=UTF-8", "smartfit-greutate-imc.csv");
    }

    @GetMapping("/setari/export/greutate.json")
    public void exportGreutateJson(Authentication auth, HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        byte[] json = settingsExportService.greutateJson(user);
        auditLogService.log(user, "EXPORT", "Export greutate & IMC (JSON)", "weight_history", null, null, request);
        writeDownload(response, json, "application/json; charset=UTF-8", "smartfit-greutate-imc.json");
    }

    @GetMapping("/setari/export/complet.zip")
    public void exportCompletZip(Authentication auth, HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        User user = getCurrentUser(auth);
        UserSettings settings = getOrCreate(user);
        byte[] zip = settingsExportService.dateCompleteZip(user, settings);
        auditLogService.log(user, "EXPORT", "Export date complete (ZIP)", "user", user.getId(), null, request);
        writeDownload(response, zip, "application/zip", "smartfit-date-complete.zip");
    }
}