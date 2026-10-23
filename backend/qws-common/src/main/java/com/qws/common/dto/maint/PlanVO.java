package com.qws.common.dto.maint;

import com.qws.common.entity.MaintPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 维保-保养计划 输出对象 */
public class PlanVO {
    private Long id;
    private String code;
    private String name;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String cycleType;
    private Integer cycleValue;
    private String startDate;
    private String nextDate;
    private String items;
    private String manager;
    private String status;
    private String remark;
    private String lastExecuteDate;
    private Integer executeCount;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static PlanVO from(MaintPlan entity) {
        PlanVO vo = new PlanVO();
        vo.id = entity.getId();
        vo.code = entity.getCode();
        vo.name = entity.getName();
        vo.equipmentId = entity.getEquipmentId();
        vo.equipmentCode = entity.getEquipmentCode();
        vo.equipmentName = entity.getEquipmentName();
        vo.cycleType = entity.getCycleType();
        vo.cycleValue = entity.getCycleValue();
        vo.startDate = formatDate(entity.getStartDate());
        vo.nextDate = formatDate(entity.getNextDate());
        vo.items = entity.getItems();
        vo.manager = entity.getManager();
        vo.status = entity.getStatus();
        vo.remark = entity.getRemark();
        vo.lastExecuteDate = formatDate(entity.getLastExecuteDate());
        vo.executeCount = entity.getExecuteCount();
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
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getNextDate() { return nextDate; }
    public void setNextDate(String nextDate) { this.nextDate = nextDate; }
    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getLastExecuteDate() { return lastExecuteDate; }
    public void setLastExecuteDate(String lastExecuteDate) { this.lastExecuteDate = lastExecuteDate; }
    public Integer getExecuteCount() { return executeCount; }
    public void setExecuteCount(Integer executeCount) { this.executeCount = executeCount; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
