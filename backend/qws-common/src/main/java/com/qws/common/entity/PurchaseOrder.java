package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("wms_purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String supplier;
    @TableField("demand_codes")
    private String demandCodes;
    @TableField("warehouse_code")
    private String warehouseCode;
    @TableField("material_count")
    private Integer materialCount;
    @TableField("total_qty")
    private BigDecimal totalQty;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    @TableField("delivery_date")
    private LocalDate deliveryDate;
    private String status;
    @TableField("inbound_code")
    private String inboundCode;
    private String creator;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getDemandCodes() { return demandCodes; }
    public void setDemandCodes(String demandCodes) { this.demandCodes = demandCodes; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public Integer getMaterialCount() { return materialCount; }
    public void setMaterialCount(Integer materialCount) { this.materialCount = materialCount; }
    public BigDecimal getTotalQty() { return totalQty; }
    public void setTotalQty(BigDecimal totalQty) { this.totalQty = totalQty; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getInboundCode() { return inboundCode; }
    public void setInboundCode(String inboundCode) { this.inboundCode = inboundCode; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
