package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 维保-设备台账 */
@TableName("maint_equipment")
public class MaintEquipment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String model;
    private String brand;
    private String category;
    @TableField("customer_id")
    private Long customerId;
    @TableField("customer_name")
    private String customerName;
    private String location;
    @TableField("install_date")
    private LocalDate installDate;
    @TableField("warranty_end")
    private LocalDate warrantyEnd;
    /** running/idle/repair/scrapped */
    private String status;
    private String manager;
    private String phone;
    @TableField("purchase_price")
    private BigDecimal purchasePrice;
    @TableField("last_maintain_date")
    private LocalDate lastMaintainDate;
    @TableField("next_maintain_date")
    private LocalDate nextMaintainDate;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
