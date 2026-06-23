package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wms_inbound_order")
public class InboundOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    @TableField("po_code")
    private String poCode;
    private String supplier;
    @TableField("warehouse_code")
    private String warehouseCode;
    @TableField("material_count")
    private Integer materialCount;
    @TableField("total_qty")
    private BigDecimal totalQty;
    @TableField("inbound_qty")
    private BigDecimal inboundQty;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    @TableField("inspect_status")
    private String inspectStatus;
    private String status;
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
    public String getPoCode() { return poCode; }
    public void setPoCode(String poCode) { this.poCode = poCode; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public Integer getMaterialCount() { return materialCount; }
    public void setMaterialCount(Integer materialCount) { this.materialCount = materialCount; }
    public BigDecimal getTotalQty() { return totalQty; }
    public void setTotalQty(BigDecimal totalQty) { this.totalQty = totalQty; }
    public BigDecimal getInboundQty() { return inboundQty; }
    public void setInboundQty(BigDecimal inboundQty) { this.inboundQty = inboundQty; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getInspectStatus() { return inspectStatus; }
    public void setInspectStatus(String inspectStatus) { this.inspectStatus = inspectStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
