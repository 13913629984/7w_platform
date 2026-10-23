package com.qws.common.dto.ets;

import com.qws.common.entity.EtsTest;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** 电测-测试执行 展示对象 */
public class TestVO {

    private Long id;
    private String code;
    private Long registerId;
    private String registerNo;
    private String customerName;
    private String product;
    private String model;
    private String testItem;
    private String category;
    private String testStandard;
    private String standardValue;
    private String measuredValue;
    private String result;
    private String tester;
    private String testTime;
    private BigDecimal durationMin;
    private String equipmentUsed;
    private String remark;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static TestVO from(EtsTest test) {
        TestVO vo = new TestVO();
        vo.id = test.getId();
        vo.code = test.getCode();
        vo.registerId = test.getRegisterId();
        vo.registerNo = test.getRegisterNo();
        vo.customerName = test.getCustomerName();
        vo.product = test.getProduct();
        vo.model = test.getModel();
        vo.testItem = test.getTestItem();
        vo.category = test.getCategory();
        vo.testStandard = test.getTestStandard();
        vo.standardValue = test.getStandardValue();
        vo.measuredValue = test.getMeasuredValue();
        vo.result = test.getResult();
        vo.tester = test.getTester();
        vo.testTime = test.getTestTime() == null ? null : test.getTestTime().format(DATETIME);
        vo.durationMin = test.getDurationMin();
        vo.equipmentUsed = test.getEquipmentUsed();
        vo.remark = test.getRemark();
        vo.createTime = test.getCreateTime() == null ? null : test.getCreateTime().format(DATETIME);
        vo.updateTime = test.getUpdateTime() == null ? null : test.getUpdateTime().format(DATETIME);
        return vo;
    }

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

    public String getTestTime() { return testTime; }
    public void setTestTime(String testTime) { this.testTime = testTime; }

    public BigDecimal getDurationMin() { return durationMin; }
    public void setDurationMin(BigDecimal durationMin) { this.durationMin = durationMin; }

    public String getEquipmentUsed() { return equipmentUsed; }
    public void setEquipmentUsed(String equipmentUsed) { this.equipmentUsed = equipmentUsed; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
