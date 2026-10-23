package com.qws.common.dto.lead;

public class LeadRequest {
    private Long id;
    private String name;
    private String source;
    private String owner;
    private String status;
    private String followAt;
    private String remark;

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
}
