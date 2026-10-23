package com.qws.common.dto.maint;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 维保-设备台账 新增/修改请求 */
public class EquipmentRequest {
    private Long id;
    private String code;
    private String name;
    private String model;
    private String brand;
    private String category;
    private Long customerId;
    private String customerName;
    private String location;
    private LocalDate installDate;
    private LocalDate warrantyEnd;
    private String status;
    private String manager;
    private String phone;
    private BigDecimal purchasePrice;
    private LocalDate lastMaintainDate;
    private LocalDate nextMaintainDate;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getInstallDate() { return installDate; }
    public void setInstallDate(LocalDate installDate) { this.installDate = installDate; }
    public LocalDate getWarrantyEnd() { return warrantyEnd; }
    public void setWarrantyEnd(LocalDate warrantyEnd) { this.warrantyEnd = warrantyEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public LocalDate getLastMaintainDate() { return lastMaintainDate; }
    public void setLastMaintainDate(LocalDate lastMaintainDate) { this.lastMaintainDate = lastMaintainDate; }
    public LocalDate getNextMaintainDate() { return nextMaintainDate; }
    public void setNextMaintainDate(LocalDate nextMaintainDate) { this.nextMaintainDate = nextMaintainDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
