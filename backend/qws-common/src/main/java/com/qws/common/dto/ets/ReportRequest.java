package com.qws.common.dto.ets;

/** 电测-测试报告 新增/修改请求 */
public class ReportRequest {

    private Long id;
    private String reportNo;
    private Long registerId;
    private String registerNo;
    private String customerName;
    private String product;
    private String model;
    private String batchNo;
    private Integer quantity;
    private Integer sampleQty;
    private Integer passQty;
    private Integer failQty;
    /** draft/pending/approved/rejected */
    private String status;
    private String conclusion;
    private String inspector;
    /** yyyy-MM-dd HH:mm */
    private String issueDate;
    private String approver;
    /** yyyy-MM-dd HH:mm */
    private String approveTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }

    public Long getRegisterId() { return registerId; }
    public void setRegisterId(Long registerId) { this.registerId = registerId; }

    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getSampleQty() { return sampleQty; }
    public void setSampleQty(Integer sampleQty) { this.sampleQty = sampleQty; }

    public Integer getPassQty() { return passQty; }
    public void setPassQty(Integer passQty) { this.passQty = passQty; }

    public Integer getFailQty() { return failQty; }
    public void setFailQty(Integer failQty) { this.failQty = failQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }

    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }

    public String getApproveTime() { return approveTime; }
    public void setApproveTime(String approveTime) { this.approveTime = approveTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
