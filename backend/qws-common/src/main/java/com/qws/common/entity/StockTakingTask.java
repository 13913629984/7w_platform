package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wms_stocktaking_task")
public class StockTakingTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("inventory_id")
    private Long inventoryId;
    @TableField("material_id")
    private Long materialId;
    @TableField("location_code")
    private String locationCode;
    @TableField("batch_no")
    private String batchNo;
    @TableField("book_qty")
    private BigDecimal bookQty;
    @TableField("actual_qty")
    private BigDecimal actualQty;
    @TableField("diff_qty")
    private BigDecimal diffQty;
    @TableField("diff_amount")
    private BigDecimal diffAmount;
    @TableField("has_diff")
    private Integer hasDiff;
    @TableField("unit_price")
    private BigDecimal unitPrice;
    private String status;
    @TableField("review_status")
    private String reviewStatus;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public BigDecimal getBookQty() { return bookQty; }
    public void setBookQty(BigDecimal bookQty) { this.bookQty = bookQty; }
    public BigDecimal getActualQty() { return actualQty; }
    public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }
    public BigDecimal getDiffQty() { return diffQty; }
    public void setDiffQty(BigDecimal diffQty) { this.diffQty = diffQty; }
    public BigDecimal getDiffAmount() { return diffAmount; }
    public void setDiffAmount(BigDecimal diffAmount) { this.diffAmount = diffAmount; }
    public Integer getHasDiff() { return hasDiff; }
    public void setHasDiff(Integer hasDiff) { this.hasDiff = hasDiff; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
