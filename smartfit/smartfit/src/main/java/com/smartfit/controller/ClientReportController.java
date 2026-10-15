package com.smartfit.controller;

import com.smartfit.dto.ClientReportStatsDto;
import com.smartfit.entity.User;
import com.smartfit.repository.UserRepository;
import com.smartfit.service.PdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/rapoarte")
public class ClientReportController {

    private final PdfExportService pdfExportService;
    private final UserRepository userRepository;

    public ClientReportController(PdfExportService pdfExportService, UserRepository userRepository) {
        this.pdfExportService = pdfExportService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showClientReports(Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName());
        if (user == null) throw new RuntimeException("Utilizatorul nu a fost găsit");

        ClientReportStatsDto stats = pdfExportService.getClientStats(user);
        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        return "rapoarte";
    }

    @PostMapping("/genereaza")
    public ResponseEntity<byte[]> generatePdf(
            @RequestParam String reportType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String antetPersonalizat,
            Authentication auth) {

        User user = userRepository.findByEmail(auth.getName());
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        byte[] pdfData = pdfExportService.generatePdf(user, reportType, start, end, antetPersonalizat);

        String filename = "SmartFit_" + reportType + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @PostMapping("/previzualizare")
    public ResponseEntity<byte[]> previewPdf(
            @RequestParam String reportType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String antetPersonalizat,
            Authentication auth) {

        User user = userRepository.findByEmail(auth.getName());
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        byte[] pdfData = pdfExportService.generatePdf(user, reportType, start, end, antetPersonalizat);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}