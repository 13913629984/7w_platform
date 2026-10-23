package com.qws.common.dto.ets;

/** 电测-测试结果录入请求（result 仅允许 pass/fail） */
public class TestResultRequest {

    private Long id;
    private String result;
    private String measuredValue;
    private String tester;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(String measuredValue) { this.measuredValue = measuredValue; }

    public String getTester() { return tester; }
    public void setTester(String tester) { this.tester = tester; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
