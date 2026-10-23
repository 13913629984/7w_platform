package com.qws.common.dto.quote;

import java.math.BigDecimal;

public class QuoteRequest {
    private Long id;
    private String customerName;
    private String brand;
    private String model;
    private BigDecimal price;
    private String visitSubject;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getVisitSubject() { return visitSubject; }
    public void setVisitSubject(String visitSubject) { this.visitSubject = visitSubject; }
}
