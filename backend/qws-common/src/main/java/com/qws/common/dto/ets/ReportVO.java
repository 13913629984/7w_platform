package com.qws.common.dto.ets;

import com.qws.common.entity.EtsReport;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** 电测-测试报告 展示对象 */
public class ReportVO {

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
    private BigDecimal passRate;
    private String status;
    private String conclusion;
    private String inspector;
    private String issueDate;
    private String approver;
    private String approveTime;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ReportVO from(EtsReport report) {
        ReportVO vo = new ReportVO();
        vo.id = report.getId();
        vo.reportNo = report.getReportNo();
        vo.registerId = report.getRegisterId();
        vo.registerNo = report.getRegisterNo();
        vo.customerName = report.getCustomerName();
        vo.product = report.getProduct();
        vo.model = report.getModel();
        vo.batchNo = report.getBatchNo();
        vo.quantity = report.getQuantity();
        vo.sampleQty = report.getSampleQty();
        vo.passQty = report.getPassQty();
        vo.failQty = report.getFailQty();
        vo.passRate = report.getPassRate();
        vo.status = report.getStatus();
        vo.conclusion = report.getConclusion();
        vo.inspector = report.getInspector();
        vo.issueDate = report.getIssueDate() == null ? null : report.getIssueDate().format(DATETIME);
        vo.approver = report.getApprover();
        vo.approveTime = report.getApproveTime() == null ? null : report.getApproveTime().format(DATETIME);
        vo.remark = report.getRemark();
        vo.createTime = report.getCreateTime() == null ? null : report.getCreateTime().format(DATETIME);
        vo.updateTime = report.getUpdateTime() == null ? null : report.getUpdateTime().format(DATETIME);
        return vo;
    }

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

    public BigDecimal getPassRate() { return passRate; }
    public void setPassRate(BigDecimal passRate) { this.passRate = passRate; }

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

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
