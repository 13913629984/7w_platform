package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 电测-来货登记 */
@TableName("ets_register")
public class EtsRegister {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("register_no")
    private String registerNo;
    @TableField("customer_name")
    private String customerName;
    private String contact;
    private String phone;
    private String product;
    private String model;
    private String brand;
    @TableField("batch_no")
    private String batchNo;
    private Integer quantity;
    @TableField("sample_qty")
    private Integer sampleQty;
    /** pending/received/testing/done/returned */
    private String status;
    @TableField("arrive_date")
    private LocalDate arriveDate;
    @TableField("expect_date")
    private LocalDate expectDate;
    @TableField("test_standard")
    private String testStandard;
    private String operator;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getSampleQty() { return sampleQty; }
    public void setSampleQty(Integer sampleQty) { this.sampleQty = sampleQty; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getArriveDate() { return arriveDate; }
    public void setArriveDate(LocalDate arriveDate) { this.arriveDate = arriveDate; }
    public LocalDate getExpectDate() { return expectDate; }
    public void setExpectDate(LocalDate expectDate) { this.expectDate = expectDate; }
    public String getTestStandard() { return testStandard; }
    public void setTestStandard(String testStandard) { this.testStandard = testStandard; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
