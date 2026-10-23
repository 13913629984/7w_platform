package com.qws.common.dto.lead;

public class LeadConvertRequest {
    private Long id;
    private String customerName;
    private String owner;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
