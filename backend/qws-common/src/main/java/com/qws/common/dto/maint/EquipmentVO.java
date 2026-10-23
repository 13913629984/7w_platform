package com.qws.common.dto.maint;

import com.qws.common.entity.MaintEquipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 维保-设备台账 输出对象 */
public class EquipmentVO {
    private Long id;
    private String code;
    private String name;
    private String model;
    private String brand;
    private String category;
    private Long customerId;
    private String customerName;
    private String location;
    private String installDate;
    private String warrantyEnd;
    private String status;
    private String manager;
    private String phone;
    private BigDecimal purchasePrice;
    private String lastMaintainDate;
    private String nextMaintainDate;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static EquipmentVO from(MaintEquipment entity) {
        EquipmentVO vo = new EquipmentVO();
        vo.id = entity.getId();
        vo.code = entity.getCode();
        vo.name = entity.getName();
        vo.model = entity.getModel();
        vo.brand = entity.getBrand();
        vo.category = entity.getCategory();
        vo.customerId = entity.getCustomerId();
        vo.customerName = entity.getCustomerName();
        vo.location = entity.getLocation();
        vo.installDate = formatDate(entity.getInstallDate());
        vo.warrantyEnd = formatDate(entity.getWarrantyEnd());
        vo.status = entity.getStatus();
        vo.manager = entity.getManager();
        vo.phone = entity.getPhone();
        vo.purchasePrice = entity.getPurchasePrice();
        vo.lastMaintainDate = formatDate(entity.getLastMaintainDate());
        vo.nextMaintainDate = formatDate(entity.getNextMaintainDate());
        vo.remark = entity.getRemark();
        vo.createTime = formatDateTime(entity.getCreateTime());
        vo.updateTime = formatDateTime(entity.getUpdateTime());
        return vo;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE);
    }

    private static String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }

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
    public String getInstallDate() { return installDate; }
    public void setInstallDate(String installDate) { this.installDate = installDate; }
    public String getWarrantyEnd() { return warrantyEnd; }
    public void setWarrantyEnd(String warrantyEnd) { this.warrantyEnd = warrantyEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public String getLastMaintainDate() { return lastMaintainDate; }
    public void setLastMaintainDate(String lastMaintainDate) { this.lastMaintainDate = lastMaintainDate; }
    public String getNextMaintainDate() { return nextMaintainDate; }
    public void setNextMaintainDate(String nextMaintainDate) { this.nextMaintainDate = nextMaintainDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
