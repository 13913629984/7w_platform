package com.qws.common.dto.workspace;

import com.qws.common.entity.CrmLead;

import java.time.format.DateTimeFormatter;

public class WorkspaceLeadVO {
    private Long id;
    private String name;
    private String source;
    private String owner;
    private String followAt;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static WorkspaceLeadVO from(CrmLead lead) {
        WorkspaceLeadVO vo = new WorkspaceLeadVO();
        vo.id = lead.getId();
        vo.name = lead.getName();
        vo.source = lead.getSource();
        vo.owner = lead.getOwner();
        vo.followAt = lead.getFollowAt() == null ? null : lead.getFollowAt().format(DATE);
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getFollowAt() { return followAt; }
    public void setFollowAt(String followAt) { this.followAt = followAt; }
}
