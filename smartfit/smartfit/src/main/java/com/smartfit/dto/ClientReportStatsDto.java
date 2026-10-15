package com.smartfit.dto;

public class ClientReportStatsDto {
    private double caloriAzi;
    private double greutateaCurenta;
    private double cheltuieliTotale;
    private int puncteAcademie;
    private double caloriChangePct;
    private double greutateChangeDelta;
    private int comenziChangeCount;
    private int xpChangeAmount;

    // Getteri și Setteri
    public double getCaloriAzi() { return caloriAzi; }
    public void setCaloriAzi(double caloriAzi) { this.caloriAzi = caloriAzi; }
    public double getGreutateaCurenta() { return greutateaCurenta; }
    public void setGreutateaCurenta(double greutateaCurenta) { this.greutateaCurenta = greutateaCurenta; }
    public double getCheltuieliTotale() { return cheltuieliTotale; }
    public void setCheltuieliTotale(double cheltuieliTotale) { this.cheltuieliTotale = cheltuieliTotale; }
    public int getPuncteAcademie() { return puncteAcademie; }
    public void setPuncteAcademie(int puncteAcademie) { this.puncteAcademie = puncteAcademie; }
    public double getCaloriChangePct() { return caloriChangePct; }
    public void setCaloriChangePct(double caloriChangePct) { this.caloriChangePct = caloriChangePct; }
    public double getGreutateChangeDelta() { return greutateChangeDelta; }
    public void setGreutateChangeDelta(double greutateChangeDelta) { this.greutateChangeDelta = greutateChangeDelta; }
    public int getComenziChangeCount() { return comenziChangeCount; }
    public void setComenziChangeCount(int comenziChangeCount) { this.comenziChangeCount = comenziChangeCount; }
    public int getXpChangeAmount() { return xpChangeAmount; }
    public void setXpChangeAmount(int xpChangeAmount) { this.xpChangeAmount = xpChangeAmount; }
}