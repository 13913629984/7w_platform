package com.qws.common.dto.ets;

import java.util.List;
import java.util.Map;

/** 电测-工作台总览 */
public class EtsOverviewVO {

    /** 本月来货数 */
    private long monthRegister;
    /** 待测试数 */
    private long pendingTest;
    /** 今日完成测试数 */
    private long todayTested;
    /** 合格率（百分比，保留 2 位小数） */
    private java.math.BigDecimal passRate;
    /** 不合格数 */
    private long failCount;
    /** 报告待审批数 */
    private long pendingReport;
    /** 近 7 日测试趋势 [{date, count}] */
    private List<Map<String, Object>> testTrend;
    /** 测试结果分布 [{name, value}] */
    private List<Map<String, Object>> resultDistribution;
    /** 测试类别分布 [{name, value}] */
    private List<Map<String, Object>> categoryDistribution;

    public long getMonthRegister() { return monthRegister; }
    public void setMonthRegister(long monthRegister) { this.monthRegister = monthRegister; }

    public long getPendingTest() { return pendingTest; }
    public void setPendingTest(long pendingTest) { this.pendingTest = pendingTest; }

    public long getTodayTested() { return todayTested; }
    public void setTodayTested(long todayTested) { this.todayTested = todayTested; }

    public java.math.BigDecimal getPassRate() { return passRate; }
    public void setPassRate(java.math.BigDecimal passRate) { this.passRate = passRate; }

    public long getFailCount() { return failCount; }
    public void setFailCount(long failCount) { this.failCount = failCount; }

    public long getPendingReport() { return pendingReport; }
    public void setPendingReport(long pendingReport) { this.pendingReport = pendingReport; }

    public List<Map<String, Object>> getTestTrend() { return testTrend; }
    public void setTestTrend(List<Map<String, Object>> testTrend) { this.testTrend = testTrend; }

    public List<Map<String, Object>> getResultDistribution() { return resultDistribution; }
    public void setResultDistribution(List<Map<String, Object>> resultDistribution) { this.resultDistribution = resultDistribution; }

    public List<Map<String, Object>> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(List<Map<String, Object>> categoryDistribution) { this.categoryDistribution = categoryDistribution; }
}
