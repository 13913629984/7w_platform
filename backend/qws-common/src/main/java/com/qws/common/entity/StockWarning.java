package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wms_stock_warning")
public class StockWarning {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("material_id")
    private Long materialId;
    @TableField("warehouse_code")
    private String warehouseCode;
    @TableField("current_qty")
    private BigDecimal currentQty;
    private Integer threshold;
    private String level;
    private String status;
    private String handler;
    @TableField("handle_remark")
    private String handleRemark;
    @TableField("handle_time")
    private LocalDateTime handleTime;
    @TableField("warning_time")
    private LocalDateTime warningTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public BigDecimal getCurrentQty() { return currentQty; }
    public void setCurrentQty(BigDecimal currentQty) { this.currentQty = currentQty; }
    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }
    public LocalDateTime getWarningTime() { return warningTime; }
    public void setWarningTime(LocalDateTime warningTime) { this.warningTime = warningTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
