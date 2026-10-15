package com.smartfit.dto;

import java.io.Serializable;

public class CheckoutFormDto implements Serializable {
    private String firstName, lastName, email, phone;
    private String address, city, county, postalCode;

    public CheckoutFormDto() {}

    public String getFirstName()  { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public String getLastName()   { return lastName; }
    public void setLastName(String v)  { this.lastName = v; }
    public String getEmail()      { return email; }
    public void setEmail(String v)     { this.email = v; }
    public String getPhone()      { return phone; }
    public void setPhone(String v)     { this.phone = v; }
    public String getAddress()    { return address; }
    public void setAddress(String v)   { this.address = v; }
    public String getCity()       { return city; }
    public void setCity(String v)      { this.city = v; }
    public String getCounty()     { return county; }
    public void setCounty(String v)    { this.county = v; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String v){ this.postalCode = v; }
}