package com.qws.common.dto.maint;

/** 维保-工单状态流转请求 */
public class WorkOrderStatusRequest {
    private Long id;
    /** 目标状态：pending/assigned/processing/completed/closed */
    private String status;
    private String handler;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
