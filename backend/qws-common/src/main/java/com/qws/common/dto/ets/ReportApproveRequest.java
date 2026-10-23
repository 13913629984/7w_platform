package com.qws.common.dto.ets;

/** 电测-报告审批请求（action 仅允许 approve/reject） */
public class ReportApproveRequest {

    private Long id;
    private String action;
    private String approver;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
