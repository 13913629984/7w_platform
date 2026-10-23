package com.qws.common.dto.lead;

import com.qws.common.entity.CrmLead;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LeadVO {
    private Long id;
    private String name;
    private String source;
    private String owner;
    private String status;
    private String followAt;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static LeadVO from(CrmLead lead) {
        LeadVO vo = new LeadVO();
        vo.id = lead.getId();
        vo.name = lead.getName();
        vo.source = lead.getSource();
        vo.owner = lead.getOwner();
        vo.status = lead.getStatus();
        vo.followAt = formatDate(lead.getFollowAt());
        vo.remark = lead.getRemark();
        vo.createTime = formatDateTime(lead.getCreateTime());
        vo.updateTime = formatDateTime(lead.getUpdateTime());
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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFollowAt() { return followAt; }
    public void setFollowAt(String followAt) { this.followAt = followAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
