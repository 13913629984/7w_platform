package com.qws.common.dto.maint;

import java.time.LocalDate;

/** 维保-保养计划 新增/修改请求 */
public class PlanRequest {
    private Long id;
    private String code;
    private String name;
    private Long equipmentId;
    private String cycleType;
    private Integer cycleValue;
    private LocalDate startDate;
    private LocalDate nextDate;
    private String items;
    private String manager;
    private String status;
    private String remark;
    private LocalDate lastExecuteDate;
    private Integer executeCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
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
}
