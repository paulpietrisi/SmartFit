package com.smartfit.dto;

/**
 * DTO pentru filtrele paginii de Rapoarte (perioadă + filtre suplimentare).
 * Folosit ca @ModelAttribute pe formularul GET din rapoarte.html.
 */
public class ReportFilterDto {

    /** Format: yyyy-MM-dd */
    private String startDate;

    /** Format: yyyy-MM-dd */
    private String endDate;

    /** Pentru comenzi: PENDING / SHIPPED / DELIVERED / CANCELLED / null=toate */
    private String orderStatus;

    /** Pentru produse: categorie / null = toate */
    private String category;

    /** Tip raport selectat: SALES, PRODUCTS, COURSES, USERS, LOGS */
    private String reportType = "SALES";

    public ReportFilterDto() {}

    public String getStartDate()              { return startDate; }
    public void setStartDate(String v)        { this.startDate = v; }
    public String getEndDate()                { return endDate; }
    public void setEndDate(String v)          { this.endDate = v; }
    public String getOrderStatus()            { return orderStatus; }
    public void setOrderStatus(String v)      { this.orderStatus = v; }
    public String getCategory()               { return category; }
    public void setCategory(String v)         { this.category = v; }
    public String getReportType()             { return reportType; }
    public void setReportType(String v)       { this.reportType = v; }
}
