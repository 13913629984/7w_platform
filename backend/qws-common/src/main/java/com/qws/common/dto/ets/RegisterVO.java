package com.qws.common.dto.ets;

import com.qws.common.entity.EtsRegister;

import java.time.format.DateTimeFormatter;

/** 电测-来货登记 展示对象 */
public class RegisterVO {

    private Long id;
    private String registerNo;
    private String customerName;
    private String contact;
    private String phone;
    private String product;
    private String model;
    private String brand;
    private String batchNo;
    private Integer quantity;
    private Integer sampleQty;
    private String status;
    private String arriveDate;
    private String expectDate;
    private String testStandard;
    private String operator;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static RegisterVO from(EtsRegister register) {
        RegisterVO vo = new RegisterVO();
        vo.id = register.getId();
        vo.registerNo = register.getRegisterNo();
        vo.customerName = register.getCustomerName();
        vo.contact = register.getContact();
        vo.phone = register.getPhone();
        vo.product = register.getProduct();
        vo.model = register.getModel();
        vo.brand = register.getBrand();
        vo.batchNo = register.getBatchNo();
        vo.quantity = register.getQuantity();
        vo.sampleQty = register.getSampleQty();
        vo.status = register.getStatus();
        vo.arriveDate = register.getArriveDate() == null ? null : register.getArriveDate().format(DATE);
        vo.expectDate = register.getExpectDate() == null ? null : register.getExpectDate().format(DATE);
        vo.testStandard = register.getTestStandard();
        vo.operator = register.getOperator();
        vo.remark = register.getRemark();
        vo.createTime = register.getCreateTime() == null ? null : register.getCreateTime().format(DATETIME);
        vo.updateTime = register.getUpdateTime() == null ? null : register.getUpdateTime().format(DATETIME);
        return vo;
    }

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

    public String getArriveDate() { return arriveDate; }
    public void setArriveDate(String arriveDate) { this.arriveDate = arriveDate; }

    public String getExpectDate() { return expectDate; }
    public void setExpectDate(String expectDate) { this.expectDate = expectDate; }

    public String getTestStandard() { return testStandard; }
    public void setTestStandard(String testStandard) { this.testStandard = testStandard; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
