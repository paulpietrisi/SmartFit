package com.smartfit.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.smartfit.entity.Order;
import com.smartfit.entity.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Rectangle;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${smartfit.mail.from}")
    private String fromAddress;

    // ── Culori brand SmartFit ──────────────────────────────────
    private static final BaseColor BLUE_DARK  = new BaseColor(10,  37,  64);
    private static final BaseColor BLUE_MID   = new BaseColor(0,  102, 204);
    private static final BaseColor BLUE_LIGHT = new BaseColor(234,243,255);
    private static final BaseColor GREEN      = new BaseColor(48, 209, 88);
    private static final BaseColor GRAY_LIGHT = new BaseColor(245,245,247);
    private static final BaseColor GRAY_TEXT  = new BaseColor(110,110,115);
    private static final BaseColor WHITE      = BaseColor.WHITE;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ════════════════════════════════════════════════════════════
    // TRIMITE EMAIL CU PDF ATAȘAT
    // ════════════════════════════════════════════════════════════
    public void sendOrderConfirmation(Order order) {
        try {
            System.out.println(">>> TRIMIT EMAIL+PDF la: " + order.getContactEmail());

            // 1. Generează PDF-ul
            byte[] pdfBytes = generateInvoicePdf(order);

            // 2. Construiește emailul
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom("SmartFit <" + fromAddress + ">");
            helper.setTo(order.getContactEmail());
            helper.setReplyTo(fromAddress);
            helper.setSubject("Comanda #ORD-" + order.getId() + " confirmata - SmartFit");
            helper.setText(buildHtml(order), true);

            // 3. Atașează PDF-ul
            helper.addAttachment(
                    "Factura_SmartFit_ORD-" + order.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf"
            );

            mailSender.send(msg);
            System.out.println(">>> EMAIL+PDF TRIMIS CU SUCCES la: " + order.getContactEmail());

        } catch (Exception e) {
            System.err.println(">>> EROARE EMAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════
    // GENERARE PDF FACTURĂ
    // ════════════════════════════════════════════════════════════
    private byte[] generateInvoicePdf(Order order) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Fonturi ─────────────────────────────────────────────
        Font fontTitle   = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   WHITE);
        Font fontSub     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(180,200,230));
        Font fontH2      = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BLUE_DARK);
        Font fontBold    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BLUE_DARK);
        Font fontNormal  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BLUE_DARK);
        Font fontSmall   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, GRAY_TEXT);
        Font fontWhiteSm = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   WHITE);
        Font fontGreen   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(20,120,40));
        Font fontBlue    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   BLUE_MID);
        Font fontTotalLg = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BLUE_MID);

        String date = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "-";
        String dateShort = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "-";

        // ════════════════════════════════════════════════════════
        // 1. HEADER — banner albastru
        // ════════════════════════════════════════════════════════
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.5f, 1f});
        header.setSpacingAfter(20);

        // Stânga — logo + titlu
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(BLUE_DARK);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(20);
        Paragraph logoTxt = new Paragraph();
        logoTxt.add(new Chunk("SmartFit\n", fontTitle));
        logoTxt.add(new Chunk("Platforma fitness & nutritie", fontSub));
        logoCell.addElement(logoTxt);
        header.addCell(logoCell);

        // Dreapta — număr factură
        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBackgroundColor(BLUE_MID);
        invoiceCell.setBorder(Rectangle.NO_BORDER);
        invoiceCell.setPadding(20);
        invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph invoiceTxt = new Paragraph();
        invoiceTxt.add(new Chunk("FACTURA\n", fontTitle));
        invoiceTxt.add(new Chunk("#ORD-" + order.getId(),
                new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, WHITE)));
        invoiceCell.addElement(invoiceTxt);
        header.addCell(invoiceCell);

        doc.add(header);

        // ════════════════════════════════════════════════════════
        // 2. INFO RÂNDURI — emitent + client + detalii
        // ════════════════════════════════════════════════════════
        PdfPTable infoTable = new PdfPTable(3);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.2f, 1.2f, 1f});
        infoTable.setSpacingAfter(20);

        // Emitent
        PdfPCell emitentCell = new PdfPCell();
        emitentCell.setBorder(Rectangle.NO_BORDER);
        emitentCell.setBackgroundColor(GRAY_LIGHT);
        emitentCell.setPadding(14);
        Paragraph emitent = new Paragraph();
        emitent.add(new Chunk("EMITENT\n",
                new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, GRAY_TEXT)));
        emitent.add(new Chunk("SmartFit S.R.L.\n", fontBold));
        emitent.add(new Chunk("Str. Fitness nr. 1\n", fontSmall));
        emitent.add(new Chunk("Bucuresti, Romania\n", fontSmall));
        emitent.add(new Chunk("CUI: RO12345678\n", fontSmall));
        emitent.add(new Chunk("smartfit@smartfit.ro", fontSmall));
        emitentCell.addElement(emitent);
        infoTable.addCell(emitentCell);

        // Client
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.NO_BORDER);
        clientCell.setBackgroundColor(BLUE_LIGHT);
        clientCell.setPadding(14);
        Paragraph client = new Paragraph();
        client.add(new Chunk("FACTURATA CATRE\n",
                new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BLUE_MID)));
        client.add(new Chunk(order.getFullName() + "\n", fontBold));
        client.add(new Chunk(order.getShippingAddress() + "\n", fontSmall));
        client.add(new Chunk("Tel: " + order.getPhone() + "\n", fontSmall));
        client.add(new Chunk(order.getContactEmail(), fontSmall));
        clientCell.addElement(client);
        infoTable.addCell(clientCell);

        // Detalii factură
        PdfPCell detailsCell = new PdfPCell();
        detailsCell.setBorder(Rectangle.NO_BORDER);
        detailsCell.setBackgroundColor(GRAY_LIGHT);
        detailsCell.setPadding(14);
        Paragraph details = new Paragraph();
        details.add(new Chunk("DETALII FACTURA\n",
                new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, GRAY_TEXT)));
        details.add(new Chunk("Nr. factura: ", fontSmall));
        details.add(new Chunk("#ORD-" + order.getId() + "\n", fontBold));
        details.add(new Chunk("Data emiterii: ", fontSmall));
        details.add(new Chunk(dateShort + "\n", fontNormal));
        details.add(new Chunk("Data scadentei: ", fontSmall));
        details.add(new Chunk(dateShort + "\n", fontNormal));
        details.add(new Chunk("Metoda plata: ", fontSmall));
        details.add(new Chunk(
                "CARD".equals(order.getPaymentMethod()) ? "Card online" : "Cash la livrare",
                fontNormal));
        detailsCell.addElement(details);
        infoTable.addCell(detailsCell);

        doc.add(infoTable);

        // ════════════════════════════════════════════════════════
        // 3. TABEL PRODUSE
        // ════════════════════════════════════════════════════════
        PdfPTable prodTable = new PdfPTable(5);
        prodTable.setWidthPercentage(100);
        prodTable.setWidths(new float[]{0.4f, 2.8f, 0.6f, 0.8f, 0.8f});
        prodTable.setSpacingAfter(0);

        // Header tabel
        String[] headers = {"Nr.", "Produs / Serviciu", "Cant.", "Pret/buc", "Total"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, fontWhiteSm));
            hCell.setBackgroundColor(BLUE_DARK);
            hCell.setPadding(10);
            hCell.setBorder(Rectangle.NO_BORDER);
            hCell.setHorizontalAlignment(h.equals("Nr.") || h.equals("Cant.")
                    ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
            prodTable.addCell(hCell);
        }

        // Rânduri produse
        int nr = 1;
        double subtotal = 0;
        boolean alternate = false;
        for (OrderItem item : order.getItems()) {
            BaseColor rowBg = alternate ? GRAY_LIGHT : WHITE;
            alternate = !alternate;
            double lineTotal = item.getLineTotal();
            subtotal += lineTotal;

            addProductRow(prodTable, rowBg, fontNormal, fontSmall,
                    String.valueOf(nr++),
                    item.getProduct().getName(),
                    item.getProduct().getCategoryName(),
                    String.valueOf(item.getQuantity()),
                    String.format("%.0f lei", item.getPriceAtPurchase()),
                    String.format("%.0f lei", lineTotal)
            );
        }

        doc.add(prodTable);

        // ════════════════════════════════════════════════════════
        // 4. SUMAR PREȚ
        // ════════════════════════════════════════════════════════
        int discountPct = order.getPointsUsed() != null ? order.getPointsUsed() : 0;
        double xpDisc   = subtotal * discountPct / 100.0;
        double shipping  = subtotal >= 200 ? 0 : 25;

        PdfPTable summaryOuter = new PdfPTable(2);
        summaryOuter.setWidthPercentage(100);
        summaryOuter.setWidths(new float[]{1f, 1f});
        summaryOuter.setSpacingBefore(2);
        summaryOuter.setSpacingAfter(20);

        // Stânga — notă XP
        PdfPCell noteCell = new PdfPCell();
        noteCell.setBorder(Rectangle.NO_BORDER);
        noteCell.setPadding(16);
        noteCell.setBackgroundColor(new BaseColor(240, 250, 240));
        Paragraph note = new Paragraph();
        note.add(new Chunk("XP castigat cu aceasta comanda\n",
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(20,120,40))));
        note.add(new Chunk("+" + order.getTotalPrice().intValue() + " XP → discount la urmatoarea comanda!\n", fontSmall));
        note.add(new Chunk("\nStatus comanda: ", fontSmall));
        note.add(new Chunk("PLATIT",
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(20,120,40))));
        noteCell.addElement(note);
        summaryOuter.addCell(noteCell);

        // Dreapta — totaluri
        PdfPCell sumCell = new PdfPCell();
        sumCell.setBorder(Rectangle.NO_BORDER);
        sumCell.setPadding(0);
        PdfPTable sumTable = new PdfPTable(2);
        sumTable.setWidthPercentage(100);

        addSumRow(sumTable, "Subtotal:", String.format("%.0f lei", subtotal),
                fontSmall, fontSmall, GRAY_LIGHT);
        addSumRow(sumTable, "Discount XP (" + discountPct + "%):",
                String.format("-%.0f lei", xpDisc), fontSmall,
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(20,120,40)),
                WHITE);
        addSumRow(sumTable, "Livrare:",
                shipping == 0 ? "Gratuit" : String.format("%.0f lei", shipping),
                fontSmall, fontSmall, GRAY_LIGHT);

        // Total final — mare și albastru
        PdfPCell totLbl = new PdfPCell(new Phrase("TOTAL DE PLATA:", fontBold));
        totLbl.setBorder(Rectangle.TOP);
        totLbl.setBorderColorTop(BLUE_MID);
        totLbl.setBorderWidthTop(2);
        totLbl.setBackgroundColor(BLUE_LIGHT);
        totLbl.setPadding(10);
        sumTable.addCell(totLbl);

        PdfPCell totVal = new PdfPCell(new Phrase(
                String.format("%.0f lei", order.getTotalPrice()), fontTotalLg));
        totVal.setBorder(Rectangle.TOP);
        totVal.setBorderColorTop(BLUE_MID);
        totVal.setBorderWidthTop(2);
        totVal.setBackgroundColor(BLUE_LIGHT);
        totVal.setPadding(10);
        totVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumTable.addCell(totVal);

        sumCell.addElement(sumTable);
        summaryOuter.addCell(sumCell);
        doc.add(summaryOuter);

        // ════════════════════════════════════════════════════════
        // 5. FOOTER
        // ════════════════════════════════════════════════════════
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(10);

        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(BLUE_DARK);
        footerCell.setBorder(Rectangle.NO_BORDER);
        footerCell.setPadding(14);
        footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph footerTxt = new Paragraph();
        footerTxt.setAlignment(Element.ALIGN_CENTER);
        footerTxt.add(new Chunk("SmartFit S.R.L.  |  smartfit.ro  |  smartfit@smartfit.ro\n", fontWhiteSm));
        footerTxt.add(new Chunk("Retur gratuit 30 zile  |  Plata securizata SSL  |  Livrare 24-48h",
                new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(150,180,220))));
        footerCell.addElement(footerTxt);
        footer.addCell(footerCell);
        doc.add(footer);

        // ════════════════════════════════════════════════════════
        // 6. NUMĂR PAGINI (watermark discret)
        // ════════════════════════════════════════════════════════
        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.saveState();
        canvas.setColorFill(new BaseColor(230, 240, 255));
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED), 60);
        canvas.showTextAligned(Element.ALIGN_CENTER, "SMARTFIT",
                297, 380, 45);
        canvas.restoreState();

        doc.close();
        return out.toByteArray();
    }

    // ── Helper: rând produs ──────────────────────────────────────
    private void addProductRow(PdfPTable table, BaseColor bg,
                               Font fontN, Font fontS,
                               String nr, String name, String cat,
                               String qty, String price, String total) {
        PdfPCell c1 = new PdfPCell(new Phrase(nr, fontS));
        c1.setBackgroundColor(bg); c1.setBorder(Rectangle.BOTTOM);
        c1.setBorderColor(new BaseColor(230,230,230));
        c1.setPadding(9); c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell();
        c2.setBackgroundColor(bg); c2.setBorder(Rectangle.BOTTOM);
        c2.setBorderColor(new BaseColor(230,230,230)); c2.setPadding(9);
        Paragraph p = new Paragraph();
        p.add(new Chunk(name + "\n", fontN));
        p.add(new Chunk(cat, fontS));
        c2.addElement(p);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(qty, fontN));
        c3.setBackgroundColor(bg); c3.setBorder(Rectangle.BOTTOM);
        c3.setBorderColor(new BaseColor(230,230,230));
        c3.setPadding(9); c3.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(price, fontN));
        c4.setBackgroundColor(bg); c4.setBorder(Rectangle.BOTTOM);
        c4.setBorderColor(new BaseColor(230,230,230)); c4.setPadding(9);
        table.addCell(c4);

        PdfPCell c5 = new PdfPCell(new Phrase(total,
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BLUE_MID)));
        c5.setBackgroundColor(bg); c5.setBorder(Rectangle.BOTTOM);
        c5.setBorderColor(new BaseColor(230,230,230)); c5.setPadding(9);
        c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c5);
    }

    // ── Helper: rând sumar ───────────────────────────────────────
    private void addSumRow(PdfPTable table, String label, String value,
                           Font fLabel, Font fValue, BaseColor bg) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, fLabel));
        lbl.setBorder(Rectangle.NO_BORDER);
        lbl.setBackgroundColor(bg); lbl.setPadding(7);
        table.addCell(lbl);

        PdfPCell val = new PdfPCell(new Phrase(value, fValue));
        val.setBorder(Rectangle.NO_BORDER);
        val.setBackgroundColor(bg); val.setPadding(7);
        val.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(val);
    }

    // ════════════════════════════════════════════════════════════
    // HTML EMAIL BODY
    // ════════════════════════════════════════════════════════════
    private String buildHtml(Order order) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            rows.append("""
                <tr>
                  <td style="padding:10px 12px;border-bottom:1px solid #eee">%s %s</td>
                  <td style="padding:10px 12px;border-bottom:1px solid #eee;text-align:center">x%d</td>
                  <td style="padding:10px 12px;border-bottom:1px solid #eee;text-align:right;font-weight:700;color:#0066CC">%.0f lei</td>
                </tr>
                """.formatted(
                    item.getProduct().getFallbackEmoji(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getLineTotal()
            ));
        }

        String date = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "-";
        String payment = "CARD".equals(order.getPaymentMethod())
                ? "Plata cu cardul (online)" : "Plata cash la livrare";

        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#ffffff">
              <div style="background:#0A2540;padding:28px 32px">
                <div style="font-size:24px;font-weight:800;color:#ffffff">SmartFit</div>
                <div style="color:#8899AA;font-size:13px;margin-top:4px">Platforma fitness si nutritie</div>
              </div>
              <div style="padding:28px 32px;background:#ffffff">
                <div style="background:#f0faf4;border:1px solid #b8ecc8;border-radius:8px;padding:16px 20px;margin-bottom:24px">
                  <div style="font-weight:700;color:#1a5c2a;font-size:15px">Comanda a fost plasata cu succes!</div>
                  <div style="color:#3a7a4a;font-size:12px;margin-top:4px">Numar comanda: <strong>#ORD-%d</strong> | %s</div>
                </div>
                <p style="color:#333;font-size:14px;line-height:1.7;margin-bottom:20px">
                  Buna, <strong>%s</strong>!<br>
                  Comanda ta a fost primita. Estimam livrarea in <strong>24-48 ore lucratoare</strong>.<br>
                  <strong>Factura PDF este atasata acestui email.</strong>
                </p>
                <table style="width:100%%;border-collapse:collapse;margin-bottom:20px">
                  <thead>
                    <tr style="background:#f5f5f5">
                      <th style="padding:10px 12px;text-align:left;font-size:11px;text-transform:uppercase;color:#888">Produs</th>
                      <th style="padding:10px 12px;text-align:center;font-size:11px;text-transform:uppercase;color:#888">Cant.</th>
                      <th style="padding:10px 12px;text-align:right;font-size:11px;text-transform:uppercase;color:#888">Pret</th>
                    </tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>
                <div style="background:#f8f8f8;border-radius:8px;padding:16px 20px;margin-bottom:20px">
                  <table style="width:100%%">
                    <tr><td style="font-size:13px;color:#666;padding:3px 0">Metoda de plata</td>
                        <td style="font-size:13px;color:#333;text-align:right">%s</td></tr>
                    <tr><td colspan="2" style="border-top:1px solid #ddd;padding-top:8px"></td></tr>
                    <tr><td style="font-size:15px;font-weight:800;color:#333;padding-top:4px">Total platit</td>
                        <td style="font-size:15px;font-weight:800;color:#0066CC;text-align:right">%.0f lei</td></tr>
                  </table>
                </div>
                <div style="background:#EAF3FF;border-radius:8px;padding:14px 20px;margin-bottom:16px">
                  <div style="font-size:11px;font-weight:700;text-transform:uppercase;color:#0066CC;margin-bottom:6px">Adresa de livrare</div>
                  <div style="font-size:13px;color:#333">%s<br><strong>%s</strong> | Tel: %s</div>
                </div>
                <div style="background:#FFF9EC;border:1px solid #FFD86E;border-radius:8px;padding:14px 20px">
                  <div style="font-size:12px;font-weight:700;color:#C07000;margin-bottom:4px">XP castigat</div>
                  <div style="font-size:13px;color:#8A5500">Ai castigat <strong>%d XP</strong> pentru aceasta comanda!</div>
                </div>
              </div>
              <div style="background:#f5f5f5;padding:14px 32px;text-align:center;border-top:1px solid #eee">
                <p style="font-size:11px;color:#999;margin:0">SmartFit | Retur gratuit 30 zile | Plata securizata SSL</p>
              </div>
            </div>
            """.formatted(
                order.getId(), date,
                order.getFullName(),
                rows.toString(),
                payment,
                order.getTotalPrice(),
                order.getShippingAddress(),
                order.getFullName(), order.getPhone(),
                order.getTotalPrice().intValue()
        );
    }
}