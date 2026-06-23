package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("wms_stocktaking_order")
public class StockTakingOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    @TableField("warehouse_code")
    private String warehouseCode;
    @TableField("scope_desc")
    private String scopeDesc;
    private String type;
    @TableField("task_count")
    private Integer taskCount;
    @TableField("completed_count")
    private Integer completedCount;
    @TableField("diff_count")
    private Integer diffCount;
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
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getScopeDesc() { return scopeDesc; }
    public void setScopeDesc(String scopeDesc) { this.scopeDesc = scopeDesc; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getTaskCount() { return taskCount; }
    public void setTaskCount(Integer taskCount) { this.taskCount = taskCount; }
    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
    public Integer getDiffCount() { return diffCount; }
    public void setDiffCount(Integer diffCount) { this.diffCount = diffCount; }
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
