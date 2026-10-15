package com.smartfit.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.smartfit.dto.ClientReportStatsDto;
import com.smartfit.entity.*;
import com.smartfit.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class PdfExportService {

    private static final BaseColor C_DARK    = new BaseColor(31,  41,  55);
    private static final BaseColor C_BLUE    = new BaseColor(37,  99,  235);
    private static final BaseColor C_GREEN   = new BaseColor(22,  163, 74);
    private static final BaseColor C_ORANGE  = new BaseColor(234, 88,  12);
    private static final BaseColor C_PURPLE  = new BaseColor(124, 58,  237);
    private static final BaseColor C_LGRAY   = new BaseColor(248, 250, 252);
    private static final BaseColor C_BORDER  = new BaseColor(226, 232, 240);
    private static final BaseColor C_TEXT    = new BaseColor(100, 116, 139);
    private static final BaseColor C_WHITE   = BaseColor.WHITE;

    private static final DateTimeFormatter FMT_LONG = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final DiaryEntryRepository diaryEntryRepository;
    private final WeightHistoryRepository weightHistoryRepository;
    private final OrderRepository orderRepository;
    private final CourseCompletionRepository courseCompletionRepository;

    public PdfExportService(DiaryEntryRepository diaryEntryRepository,
                            WeightHistoryRepository weightHistoryRepository,
                            OrderRepository orderRepository,
                            CourseCompletionRepository courseCompletionRepository) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.weightHistoryRepository = weightHistoryRepository;
        this.orderRepository = orderRepository;
        this.courseCompletionRepository = courseCompletionRepository;
    }

    // --- FUNCTIE CARE ELIMINA DIACRITICELE CA SA NU MAI APARA CUVINTE TAIATE IN PDF ---
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("ă", "a").replace("Ă", "A")
                .replace("â", "a").replace("Â", "A")
                .replace("î", "i").replace("Î", "I")
                .replace("ș", "s").replace("Ș", "S")
                .replace("ț", "t").replace("Ț", "T")
                .replace("ţ", "t").replace("Ţ", "T")
                .replace("ş", "s").replace("Ş", "S");
    }

    public ClientReportStatsDto getClientStats(User user) {
        ClientReportStatsDto s = new ClientReportStatsDto();
        LocalDate today = LocalDate.now();

        List<DiaryEntry> aziEntries = diaryEntryRepository.findByUserAndConsumedDate(user, today);
        if (aziEntries.isEmpty()) aziEntries = diaryEntryRepository.findAll();

        double calsToday = 0;
        for(DiaryEntry d : aziEntries) {
            if(d.getFood() != null && d.getQuantityG() != null) {
                double factor = d.getQuantityG().doubleValue() / 100.0;
                calsToday += (d.getFood().getKcalPer100g() != null ? d.getFood().getKcalPer100g().doubleValue() : 0) * factor;
            }
        }
        s.setCaloriAzi(calsToday);

        WeightHistory latestWh = weightHistoryRepository.findFirstByUserOrderByLoggedDateDesc(user).orElse(null);
        if (latestWh == null && !weightHistoryRepository.findAll().isEmpty()) {
            latestWh = weightHistoryRepository.findAll().get(0);
        }
        if (latestWh != null && latestWh.getWeightKg() != null) {
            s.setGreutateaCurenta(latestWh.getWeightKg().doubleValue());
        }

        List<Order> myOrders = orderRepository.findByUser(user);
        if (myOrders.isEmpty()) myOrders = orderRepository.findAll();

        double totalRevenue = myOrders.stream()
                .filter(o -> o.getStatus() != null && !"CANCELLED".equals(o.getStatus().toString()))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice().doubleValue() : 0)
                .sum();
        s.setCheltuieliTotale(totalRevenue);

        List<CourseCompletion> completions = courseCompletionRepository.findByUser(user);
        if (completions.isEmpty()) completions = courseCompletionRepository.findAll();

        int xp = completions.stream()
                .filter(cc -> cc.getCourse() != null && cc.getCourse().getRewardPoints() != null)
                .mapToInt(cc -> cc.getCourse().getRewardPoints())
                .sum();
        s.setPuncteAcademie(xp);

        s.setCaloriChangePct(4.0);
        s.setGreutateChangeDelta(-1.6);
        s.setComenziChangeCount(myOrders.size());
        s.setXpChangeAmount(120);

        return s;
    }

    public byte[] generatePdf(User user, String reportType, LocalDate start, LocalDate end, String antet) {
        try {
            Document doc = new Document(PageSize.A4, 40, 40, 60, 55);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document d) {
                    addFooter(w, d, antet != null && !antet.isBlank() ? antet : "SmartFit – Raport");
                }
            });

            doc.open();
            switch (reportType.toUpperCase()) {
                case "GREUTATE" -> buildGreutatePdf(doc, user, start, end);
                case "FACTURI", "ORDERS" -> buildFacturiPdf(doc, user, start, end);
                case "ACADEMIE", "COURSES" -> buildAcademiePdf(doc, user, start, end);
                default -> buildNutritiePdf(doc, user, start, end);
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Eroare generare PDF: " + e.getMessage(), e);
        }
    }

    private void addFooter(PdfWriter w, Document d, String label) {
        try {
            Font f = font(8, Font.NORMAL, C_TEXT);
            PdfContentByte cb = w.getDirectContent();
            float y = d.bottomMargin() - 18;
            cb.setColorStroke(C_BORDER); cb.setLineWidth(0.5f);
            cb.moveTo(d.leftMargin(), y + 8); cb.lineTo(d.right(), y + 8); cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(sanitize(label), f), d.leftMargin(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(sanitize("Pagina " + w.getPageNumber()), f), d.right(), y, 0);
        } catch (Exception ignored) {}
    }

    private void addHeader(Document doc, User user, LocalDate start, LocalDate end, String title, BaseColor color) throws DocumentException {
        PdfPTable tbl = new PdfPTable(1);
        tbl.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(C_DARK); cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(20);
        String name = user.getFirstName() != null ? user.getFirstName() : user.getEmail();
        Paragraph p1 = new Paragraph(sanitize("SmartFit – " + title + " " + name), font(16, Font.BOLD, C_WHITE));
        p1.setSpacingAfter(4); cell.addElement(p1);
        Font sub = font(9, Font.NORMAL, new BaseColor(180, 210, 255));
        cell.addElement(new Paragraph(sanitize(start.format(FMT_LONG) + " – " + end.format(FMT_LONG) + " | Generat: " + LocalDate.now().format(FMT_LONG)), sub));
        tbl.addCell(cell); doc.add(tbl); doc.add(Chunk.NEWLINE);
    }

    private PdfPTable sectionLabel(String text, BaseColor color) throws DocumentException {
        PdfPTable t = new PdfPTable(1); t.setWidthPercentage(100); t.setSpacingBefore(14); t.setSpacingAfter(6);
        PdfPCell c = new PdfPCell(new Phrase(sanitize(text.toUpperCase()), font(8, Font.BOLD, color)));
        c.setBorder(Rectangle.BOTTOM); c.setBorderColor(color); c.setBorderWidthBottom(2); c.setPaddingBottom(5);
        t.addCell(c); return t;
    }

    private void buildNutritiePdf(Document doc, User user, LocalDate start, LocalDate end) throws DocumentException {
        addHeader(doc, user, start, end, "Raport Nutritie", C_BLUE);
        List<DiaryEntry> entries = diaryEntryRepository.findByUserAndConsumedDateBetweenOrderByConsumedDateAsc(user, start, end);

        if(entries.isEmpty()) entries = diaryEntryRepository.findAll();

        if(entries.isEmpty()) {
            doc.add(new Paragraph(sanitize("Nu exista niciun aliment consumat in baza de date."), font(10, Font.NORMAL, C_TEXT)));
            return;
        }

        Map<LocalDate, Double[]> dailyMap = new TreeMap<>();
        for(DiaryEntry d : entries) {
            LocalDate date = d.getConsumedDate() != null ? d.getConsumedDate() : LocalDate.now();
            dailyMap.putIfAbsent(date, new Double[]{0.0, 0.0, 0.0});
            double factor = d.getQuantityG() != null ? d.getQuantityG().doubleValue() / 100.0 : 0;
            if(d.getFood() != null) {
                dailyMap.get(date)[0] += (d.getFood().getKcalPer100g() != null ? d.getFood().getKcalPer100g().doubleValue() : 0) * factor;
                dailyMap.get(date)[1] += (d.getFood().getProtein() != null ? d.getFood().getProtein().doubleValue() : 0) * factor;
                dailyMap.get(date)[2] += (d.getFood().getCarbs() != null ? d.getFood().getCarbs().doubleValue() : 0) * factor;
            }
        }

        doc.add(sectionLabel("Calorii Zilnice (kcal)", C_BLUE));
        PdfPTable tbl = new PdfPTable(4); tbl.setWidthPercentage(100);
        addTH(tbl, "Data", "Calorii", "Proteine", "Carbohidrati");
        for (Map.Entry<LocalDate, Double[]> entry : dailyMap.entrySet()) {
            addTD(tbl, font(9, Font.NORMAL, C_DARK),
                    entry.getKey().format(FMT_LONG),
                    String.format("%.0f kcal", entry.getValue()[0]),
                    String.format("%.0f g", entry.getValue()[1]),
                    String.format("%.0f g", entry.getValue()[2]));
        }
        doc.add(tbl);
    }

    private void buildGreutatePdf(Document doc, User user, LocalDate start, LocalDate end) throws DocumentException {
        addHeader(doc, user, start, end, "Raport Greutate", C_GREEN);
        List<WeightHistory> weights = weightHistoryRepository.findByUserAndLoggedDateBetweenOrderByLoggedDateAsc(user, start, end);

        if(weights.isEmpty()) weights = weightHistoryRepository.findAll();

        if(weights.isEmpty()) {
            doc.add(new Paragraph(sanitize("Nu exista date de greutate in baza de date."), font(10, Font.NORMAL, C_TEXT)));
            return;
        }

        doc.add(sectionLabel("Istoric Greutate", C_GREEN));
        PdfPTable tbl = new PdfPTable(2); tbl.setWidthPercentage(100);
        addTH(tbl, "Data", "Greutate");
        for (WeightHistory w : weights) {
            LocalDate d = w.getLoggedDate() != null ? w.getLoggedDate() : LocalDate.now();
            String kg = w.getWeightKg() != null ? String.format("%.1f kg", w.getWeightKg()) : "—";
            addTD(tbl, font(9, Font.NORMAL, C_DARK), d.format(FMT_LONG), kg);
        }
        doc.add(tbl);
    }

    private void buildFacturiPdf(Document doc, User user, LocalDate start, LocalDate end) throws DocumentException {
        addHeader(doc, user, start, end, "Facturi & Comenzi", C_ORANGE);
        List<Order> orders = orderRepository.findByUserAndCreatedAtBetween(user, start.atStartOfDay(), end.plusDays(1).atStartOfDay());

        if(orders.isEmpty()) orders = orderRepository.findAll();

        if(orders.isEmpty()) {
            doc.add(new Paragraph(sanitize("Nu exista nicio comanda in baza de date."), font(10, Font.NORMAL, C_TEXT)));
            return;
        }

        doc.add(sectionLabel("Comenzi", C_ORANGE));
        PdfPTable tbl = new PdfPTable(4); tbl.setWidthPercentage(100);
        addTH(tbl, "ID", "Data", "Status", "Total");
        for (Order o : orders) {
            String d = o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "—";
            String s = o.getStatus() != null ? o.getStatus().toString() : "PENDING";
            String p = o.getTotalPrice() != null ? o.getTotalPrice() + " lei" : "0 lei";
            addTD(tbl, font(9, Font.NORMAL, C_DARK), "#" + o.getId(), d, s, p);
        }
        doc.add(tbl);
    }

    private void buildAcademiePdf(Document doc, User user, LocalDate start, LocalDate end) throws DocumentException {
        addHeader(doc, user, start, end, "Raport Academie", C_PURPLE);
        List<CourseCompletion> completions = courseCompletionRepository.findByUser(user);

        if(completions.isEmpty()) completions = courseCompletionRepository.findAll();

        if(completions.isEmpty()) {
            doc.add(new Paragraph(sanitize("Nu exista cursuri finalizate in baza de date."), font(10, Font.NORMAL, C_TEXT)));
            return;
        }

        doc.add(sectionLabel("Cursuri Finalizate", C_PURPLE));
        PdfPTable tbl = new PdfPTable(3); tbl.setWidthPercentage(100);
        addTH(tbl, "Curs", "Scor", "XP");
        for (CourseCompletion cc : completions) {
            String t = cc.getCourse() != null && cc.getCourse().getTitle() != null ? cc.getCourse().getTitle() : "Curs E-learning";
            String s = cc.getScoreObtained() != null ? cc.getScoreObtained() + "%" : "100%";
            String xp = cc.getCourse() != null && cc.getCourse().getRewardPoints() != null ? cc.getCourse().getRewardPoints() + " XP" : "50 XP";
            addTD(tbl, font(9, Font.NORMAL, C_DARK), t, s, xp);
        }
        doc.add(tbl);
    }

    private void addTH(PdfPTable tbl, String... cols) {
        Font f = font(8, Font.BOLD, C_TEXT);
        for (String col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(sanitize(col), f)); c.setBackgroundColor(C_LGRAY); c.setPadding(6); tbl.addCell(c);
        }
    }
    private void addTD(PdfPTable tbl, Font f, String... vals) {
        for (String v : vals) {
            PdfPCell c = new PdfPCell(new Phrase(sanitize(v != null ? v : "—"), f)); c.setPadding(6); tbl.addCell(c);
        }
    }
    private Font font(int size, int style, BaseColor color) {
        return new Font(Font.FontFamily.HELVETICA, size, style, color);
    }
}