package com.smartfit.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Font;
import com.smartfit.entity.Order;
import com.smartfit.entity.OrderItem;
import com.smartfit.entity.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generează exporturi PDF (rapoarte / facturi / chitanțe) și Excel (XLSX)
 * pentru panourile admin/angajat.
 *
 * Reutilizează aceleași culori de brand ca în EmailService pentru consistență vizuală.
 */
@Service
public class ExportService {

    private static final BaseColor BLUE_DARK  = new BaseColor(10,  37,  64);
    private static final BaseColor BLUE_MID   = new BaseColor(0,  102, 204);
    private static final BaseColor BLUE_LIGHT = new BaseColor(234,243,255);
    private static final BaseColor GRAY_LIGHT = new BaseColor(245,245,247);
    private static final BaseColor GRAY_TEXT  = new BaseColor(110,110,115);
    private static final BaseColor WHITE      = BaseColor.WHITE;

    // ════════════════════════════════════════════════════════════
    // 1. EXPORT GENERIC — TABEL → PDF (rapoarte)
    // ════════════════════════════════════════════════════════════

    /**
     * Generează un PDF cu un tabel generic (titlu + header + rânduri).
     * Folosit pentru: raport vânzări, raport produse, raport useri, log-uri etc.
     *
     * @param title       titlul raportului (ex. "Raport vânzări 01.06.2026 - 13.06.2026")
     * @param headers     anteturile coloanelor
     * @param rows        rândurile de date (fiecare rând = listă de String)
     * @param columnWidths lățimi relative ale coloanelor (trebuie să aibă aceeași lungime ca headers)
     */
    public byte[] exportTableToPdf(String title, List<String> headers,
                                    List<List<String>> rows, float[] columnWidths) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fontTitle  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BLUE_DARK);
            Font fontSub    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, GRAY_TEXT);
            Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, WHITE);
            Font fontCell   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BLUE_DARK);

            // Header banner
            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(BLUE_DARK);
            bannerCell.setBorder(Rectangle.NO_BORDER);
            bannerCell.setPadding(16);
            Paragraph p = new Paragraph();
            p.add(new Chunk("SmartFit\n", new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, WHITE)));
            p.add(new Chunk("Platforma fitness si nutritie - Raport administrativ",
                    new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(180,200,230))));
            bannerCell.addElement(p);
            banner.addCell(bannerCell);
            doc.add(banner);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(title, fontTitle));
            doc.add(new Paragraph("Generat la: " + java.time.LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), fontSub));
            doc.add(new Paragraph(" "));

            // Table
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            if (columnWidths != null && columnWidths.length == headers.size()) {
                table.setWidths(columnWidths);
            }

            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
                c.setBackgroundColor(BLUE_MID);
                c.setPadding(8);
                c.setBorder(Rectangle.NO_BORDER);
                table.addCell(c);
            }

            boolean alt = false;
            for (List<String> row : rows) {
                BaseColor bg = alt ? GRAY_LIGHT : WHITE;
                alt = !alt;
                for (String val : row) {
                    PdfPCell c = new PdfPCell(new Phrase(val != null ? val : "-", fontCell));
                    c.setBackgroundColor(bg);
                    c.setPadding(6);
                    c.setBorder(Rectangle.BOTTOM);
                    c.setBorderColor(new BaseColor(230, 230, 230));
                    table.addCell(c);
                }
            }

            doc.add(table);

            // Footer
            doc.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("SmartFit S.R.L. | smartfit.ro | Document generat automat",
                    new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, GRAY_TEXT));
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Eroare la generarea PDF-ului: " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. EXPORT FACTURĂ / CHITANȚĂ PENTRU O COMANDĂ (reutilizat din EmailService)
    // ════════════════════════════════════════════════════════════

    /**
     * Generează factura PDF pentru o comandă — folosit la export individual
     * din pagina de administrare comenzi ("Descarcă factura").
     */
    public byte[] exportInvoicePdf(Order order) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fontTitle  = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, WHITE);
            Font fontSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(180,200,230));
            Font fontBold   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BLUE_DARK);
            Font fontNormal = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BLUE_DARK);
            Font fontSmall  = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
            Font fontWhite  = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WHITE);
            Font fontTotal  = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BLUE_MID);

            String dateShort = order.getCreatedAt() != null
                    ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : "-";

            // Header
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.5f, 1f});
            header.setSpacingAfter(20);

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBackgroundColor(BLUE_DARK);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(20);
            Paragraph logoTxt = new Paragraph();
            logoTxt.add(new Chunk("SmartFit\n", fontTitle));
            logoTxt.add(new Chunk("Platforma fitness & nutritie", fontSub));
            logoCell.addElement(logoTxt);
            header.addCell(logoCell);

            PdfPCell invCell = new PdfPCell();
            invCell.setBackgroundColor(BLUE_MID);
            invCell.setBorder(Rectangle.NO_BORDER);
            invCell.setPadding(20);
            invCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph invTxt = new Paragraph();
            invTxt.add(new Chunk("FACTURA\n", fontTitle));
            invTxt.add(new Chunk("#ORD-" + order.getId(),
                    new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, WHITE)));
            invCell.addElement(invTxt);
            header.addCell(invCell);
            doc.add(header);

            // Info
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setSpacingAfter(20);

            PdfPCell clientCell = new PdfPCell();
            clientCell.setBorder(Rectangle.NO_BORDER);
            clientCell.setBackgroundColor(BLUE_LIGHT);
            clientCell.setPadding(14);
            Paragraph client = new Paragraph();
            client.add(new Chunk("FACTURAT CATRE\n", new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BLUE_MID)));
            client.add(new Chunk(order.getFullName() + "\n", fontBold));
            client.add(new Chunk(order.getShippingAddress() + "\n", fontSmall));
            client.add(new Chunk("Tel: " + order.getPhone() + "\n", fontSmall));
            client.add(new Chunk(order.getContactEmail(), fontSmall));
            clientCell.addElement(client);
            info.addCell(clientCell);

            PdfPCell detCell = new PdfPCell();
            detCell.setBorder(Rectangle.NO_BORDER);
            detCell.setBackgroundColor(GRAY_LIGHT);
            detCell.setPadding(14);
            Paragraph det = new Paragraph();
            det.add(new Chunk("DETALII\n", new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, GRAY_TEXT)));
            det.add(new Chunk("Data: " + dateShort + "\n", fontNormal));
            det.add(new Chunk("Status: " + order.getStatus() + "\n", fontNormal));
            det.add(new Chunk("Plata: " + ("CARD".equals(order.getPaymentMethod()) ? "Card online" : "Cash"), fontNormal));
            detCell.addElement(det);
            info.addCell(detCell);
            doc.add(info);

            // Produse
            PdfPTable prod = new PdfPTable(4);
            prod.setWidthPercentage(100);
            prod.setWidths(new float[]{3f, 0.7f, 1f, 1f});
            String[] heads = {"Produs", "Cant.", "Pret/buc", "Total"};
            for (String h : heads) {
                PdfPCell c = new PdfPCell(new Phrase(h, fontWhite));
                c.setBackgroundColor(BLUE_DARK);
                c.setPadding(8);
                c.setBorder(Rectangle.NO_BORDER);
                prod.addCell(c);
            }
            double subtotal = 0;
            for (OrderItem item : order.getItems()) {
                subtotal += item.getLineTotal();
                prod.addCell(cell(item.getProduct().getName(), fontNormal, WHITE));
                prod.addCell(cell(String.valueOf(item.getQuantity()), fontNormal, WHITE));
                prod.addCell(cell(String.format("%.2f lei", item.getPriceAtPurchase()), fontNormal, WHITE));
                prod.addCell(cell(String.format("%.2f lei", item.getLineTotal()), fontBold, WHITE));
            }
            doc.add(prod);

            doc.add(new Paragraph(" "));
            Paragraph total = new Paragraph("TOTAL: " + String.format("%.2f lei", order.getTotalPrice()), fontTotal);
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Eroare la generarea facturii: " + e.getMessage(), e);
        }
    }

    private PdfPCell cell(String text, Font font, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setPadding(6);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new BaseColor(230,230,230));
        return c;
    }

    // ════════════════════════════════════════════════════════════
    // 3. EXPORT XLSX — generic
    // ════════════════════════════════════════════════════════════

    /**
     * Generează un fișier XLSX generic (titlu sheet + header + rânduri).
     * Folosit pentru export produse, comenzi, useri, log-uri.
     */
    public byte[] exportTableToExcel(String sheetName, List<String> headers, List<List<String>> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (List<String> row : rows) {
                Row r = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.size(); i++) {
                    Cell c = r.createCell(i);
                    String val = row.get(i);
                    // Încearcă să scrie ca număr dacă e posibil, altfel ca text
                    if (val != null && val.matches("-?\\d+(\\.\\d+)?")) {
                        c.setCellValue(Double.parseDouble(val));
                    } else {
                        c.setCellValue(val != null ? val : "");
                    }
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Eroare la generarea fisierului Excel: " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════
    // 4. EXPORT PRODUSE — convenience (folosit la "Export produse")
    // ════════════════════════════════════════════════════════════

    public byte[] exportProductsExcel(List<Product> products) {
        List<String> headers = List.of("ID", "Nume", "Categorie", "Preț (lei)", "Stoc", "Descriere");
        List<List<String>> rows = products.stream().map(p -> List.of(
                String.valueOf(p.getId()),
                p.getName(),
                p.getCategoryName(),
                String.valueOf(p.getPrice()),
                String.valueOf(p.getStockQuantity()),
                p.getDescription() != null ? p.getDescription() : ""
        )).toList();
        return exportTableToExcel("Produse", headers, rows);
    }
}
