package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("operate_time")
    private LocalDateTime operateTime;
    private String username;
    @TableField("module_code")
    private String moduleCode;
    @TableField("module_name")
    private String moduleName;
    @TableField("action_type")
    private String actionType;
    @TableField("action_name")
    private String actionName;
    private String content;
    @TableField("target_name")
    private String targetName;
    @TableField("ip_address")
    private String ipAddress;
    private String status;
    @TableField("cost_ms")
    private Integer costMs;
    @TableField("request_id")
    private String requestId;
    private String browser;
    private String detail;
    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
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
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
