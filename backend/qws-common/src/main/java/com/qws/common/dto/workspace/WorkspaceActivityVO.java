package com.qws.common.dto.workspace;

import com.qws.common.entity.CrmActivity;

import java.time.format.DateTimeFormatter;

public class WorkspaceActivityVO {
    private Long id;
    private String title;
    private String type;
    private String customerName;
    private String owner;
    private String activityAt;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static WorkspaceActivityVO from(CrmActivity activity) {
        WorkspaceActivityVO vo = new WorkspaceActivityVO();
        vo.id = activity.getId();
        vo.title = activity.getTitle();
        vo.type = activity.getType();
        vo.customerName = activity.getCustomerName();
        vo.owner = activity.getOwner();
        vo.activityAt = activity.getActivityAt() == null ? null : activity.getActivityAt().format(DATETIME);
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getActivityAt() { return activityAt; }
    public void setActivityAt(String activityAt) { this.activityAt = activityAt; }
}
