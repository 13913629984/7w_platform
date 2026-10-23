package com.qws.common.dto.activity;

import com.qws.common.entity.CrmActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityVO {
    private Long id;
    private String title;
    private String type;
    private Long customerId;
    private String customerName;
    private String owner;
    private String activityAt;
    private String status;
    private String remark;
    private String createTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ActivityVO from(CrmActivity entity) {
        ActivityVO vo = new ActivityVO();
        vo.id = entity.getId();
        vo.title = entity.getTitle();
        vo.type = entity.getType();
        vo.customerId = entity.getCustomerId();
        vo.customerName = entity.getCustomerName();
        vo.owner = entity.getOwner();
        vo.activityAt = format(entity.getActivityAt());
        vo.status = entity.getStatus();
        vo.remark = entity.getRemark();
        vo.createTime = format(entity.getCreateTime());
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getActivityAt() { return activityAt; }
    public void setActivityAt(String activityAt) { this.activityAt = activityAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
