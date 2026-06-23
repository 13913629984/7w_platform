package com.qws.common.dto.audit;

public class AuditLogVO {
    private Long id;
    private String operateTime;
    private String username;
    private String moduleCode;
    private String moduleName;
    private String actionType;
    private String actionName;
    private String content;
    private String targetName;
    private String ipAddress;
    private String status;
    private Integer costMs;
    private String costText;
    private String requestId;
    private String browser;
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperateTime() { return operateTime; }
    public void setOperateTime(String operateTime) { this.operateTime = operateTime; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCostMs() { return costMs; }
    public void setCostMs(Integer costMs) { this.costMs = costMs; }
    public String getCostText() { return costText; }
    public void setCostText(String costText) { this.costText = costText; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
