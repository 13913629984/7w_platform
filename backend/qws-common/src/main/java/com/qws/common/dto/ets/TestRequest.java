package com.qws.common.dto.ets;

/** 电测-测试执行 新增/修改请求 */
public class TestRequest {

    private Long id;
    private String code;
    private Long registerId;
    private String registerNo;
    private String customerName;
    private String product;
    private String model;
    private String testItem;
    /** appearance/insulation/withstand/grounding/temperature/performance */
    private String category;
    private String testStandard;
    private String standardValue;
    private String measuredValue;
    /** pass/fail/pending */
    private String result;
    private String tester;
    /** yyyy-MM-dd HH:mm */
    private String testTime;
    private String durationMin;
    private String equipmentUsed;
    private String remark;

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

    public String getDurationMin() { return durationMin; }
    public void setDurationMin(String durationMin) { this.durationMin = durationMin; }

    public String getEquipmentUsed() { return equipmentUsed; }
    public void setEquipmentUsed(String equipmentUsed) { this.equipmentUsed = equipmentUsed; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
