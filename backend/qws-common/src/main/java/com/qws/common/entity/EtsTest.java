package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 电测-测试执行 */
@TableName("ets_test")
public class EtsTest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    @TableField("register_id")
    private Long registerId;
    @TableField("register_no")
    private String registerNo;
    @TableField("customer_name")
    private String customerName;
    private String product;
    private String model;
    @TableField("test_item")
    private String testItem;
    /** appearance/insulation/withstand/grounding/temperature/performance */
    private String category;
    @TableField("test_standard")
    private String testStandard;
    @TableField("standard_value")
    private String standardValue;
    @TableField("measured_value")
    private String measuredValue;
    /** pass/fail/pending */
    private String result;
    private String tester;
    @TableField("test_time")
    private LocalDateTime testTime;
    @TableField("duration_min")
    private BigDecimal durationMin;
    @TableField("equipment_used")
    private String equipmentUsed;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
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
    public String getTestItem() { return testItem; }
    public void setTestItem(String testItem) { this.testItem = testItem; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTestStandard() { return testStandard; }
    public void setTestStandard(String testStandard) { this.testStandard = testStandard; }
    public String getStandardValue() { return standardValue; }
    public void setStandardValue(String standardValue) { this.standardValue = standardValue; }
    public String getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(String measuredValue) { this.measuredValue = measuredValue; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getTester() { return tester; }
    public void setTester(String tester) { this.tester = tester; }
    public LocalDateTime getTestTime() { return testTime; }
    public void setTestTime(LocalDateTime testTime) { this.testTime = testTime; }
    public BigDecimal getDurationMin() { return durationMin; }
    public void setDurationMin(BigDecimal durationMin) { this.durationMin = durationMin; }
    public String getEquipmentUsed() { return equipmentUsed; }
    public void setEquipmentUsed(String equipmentUsed) { this.equipmentUsed = equipmentUsed; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
