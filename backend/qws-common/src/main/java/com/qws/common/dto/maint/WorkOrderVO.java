package com.qws.common.dto.maint;

import com.qws.common.entity.MaintWorkOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 维保-维修工单 输出对象 */
public class WorkOrderVO {
    private Long id;
    private String code;
    private String title;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String customerName;
    private String type;
    private String priority;
    private String status;
    private String reporter;
    private String handler;
    private String reportTime;
    private String planTime;
    private String finishTime;
    private BigDecimal hours;
    private BigDecimal cost;
    private String faultDesc;
    private String solution;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static WorkOrderVO from(MaintWorkOrder entity) {
        WorkOrderVO vo = new WorkOrderVO();
        vo.id = entity.getId();
        vo.code = entity.getCode();
        vo.title = entity.getTitle();
        vo.equipmentId = entity.getEquipmentId();
        vo.equipmentCode = entity.getEquipmentCode();
        vo.equipmentName = entity.getEquipmentName();
        vo.customerName = entity.getCustomerName();
        vo.type = entity.getType();
        vo.priority = entity.getPriority();
        vo.status = entity.getStatus();
        vo.reporter = entity.getReporter();
        vo.handler = entity.getHandler();
        vo.reportTime = format(entity.getReportTime());
        vo.planTime = format(entity.getPlanTime());
        vo.finishTime = format(entity.getFinishTime());
        vo.hours = entity.getHours();
        vo.cost = entity.getCost();
        vo.faultDesc = entity.getFaultDesc();
        vo.solution = entity.getSolution();
        vo.remark = entity.getRemark();
        vo.createTime = format(entity.getCreateTime());
        vo.updateTime = format(entity.getUpdateTime());
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public String getReportTime() { return reportTime; }
    public void setReportTime(String reportTime) { this.reportTime = reportTime; }
    public String getPlanTime() { return planTime; }
    public void setPlanTime(String planTime) { this.planTime = planTime; }
    public String getFinishTime() { return finishTime; }
    public void setFinishTime(String finishTime) { this.finishTime = finishTime; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getFaultDesc() { return faultDesc; }
    public void setFaultDesc(String faultDesc) { this.faultDesc = faultDesc; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
