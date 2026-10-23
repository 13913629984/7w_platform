package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 维保-保养计划 */
@TableName("maint_plan")
public class MaintPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    @TableField("equipment_id")
    private Long equipmentId;
    @TableField("equipment_code")
    private String equipmentCode;
    @TableField("equipment_name")
    private String equipmentName;
    /** day/week/month/quarter/year */
    @TableField("cycle_type")
    private String cycleType;
    @TableField("cycle_value")
    private Integer cycleValue;
    @TableField("start_date")
    private LocalDate startDate;
    @TableField("next_date")
    private LocalDate nextDate;
    /** 保养项目，逗号分隔 */
    private String items;
    private String manager;
    /** active/paused/finished */
    private String status;
    private String remark;
    @TableField("last_execute_date")
    private LocalDate lastExecuteDate;
    @TableField("execute_count")
    private Integer executeCount;
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
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getCycleType() { return cycleType; }
    public void setCycleType(String cycleType) { this.cycleType = cycleType; }
    public Integer getCycleValue() { return cycleValue; }
    public void setCycleValue(Integer cycleValue) { this.cycleValue = cycleValue; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getNextDate() { return nextDate; }
    public void setNextDate(LocalDate nextDate) { this.nextDate = nextDate; }
    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDate getLastExecuteDate() { return lastExecuteDate; }
    public void setLastExecuteDate(LocalDate lastExecuteDate) { this.lastExecuteDate = lastExecuteDate; }
    public Integer getExecuteCount() { return executeCount; }
    public void setExecuteCount(Integer executeCount) { this.executeCount = executeCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
