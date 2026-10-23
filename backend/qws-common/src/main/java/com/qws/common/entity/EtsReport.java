package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 电测-测试报告 */
@TableName("ets_report")
public class EtsReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("report_no")
    private String reportNo;
    @TableField("register_id")
    private Long registerId;
    @TableField("register_no")
    private String registerNo;
    @TableField("customer_name")
    private String customerName;
    private String product;
    private String model;
    @TableField("batch_no")
    private String batchNo;
    private Integer quantity;
    @TableField("sample_qty")
    private Integer sampleQty;
    @TableField("pass_qty")
    private Integer passQty;
    @TableField("fail_qty")
    private Integer failQty;
    @TableField("pass_rate")
    private BigDecimal passRate;
    /** draft/pending/approved/rejected */
    private String status;
    private String conclusion;
    private String inspector;
    @TableField("issue_date")
    private LocalDateTime issueDate;
    @TableField("approver")
    private String approver;
    @TableField("approve_time")
    private LocalDateTime approveTime;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

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
    public LocalDateTime getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
