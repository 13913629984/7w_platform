package com.qws.common.dto.maint;

import java.util.List;
import java.util.Map;

/** 维保-工作台汇总数据 */
public class MaintOverviewVO {

    /** 设备总数 */
    private long equipmentTotal;
    /** 运行中设备数 */
    private long runningCount;
    /** 维修中设备数 */
    private long repairCount;
    /** 待处理工单数 */
    private long pendingOrderCount;
    /** 逾期工单数 */
    private long overdueOrderCount;
    /** 本月完成工单数 */
    private long monthCompletedCount;
    /** 计划到期数（7 天内） */
    private long planDueCount;
    /** 备件低于安全库存数 */
    private long lowStockCount;
    /** 近 7 日工单趋势 */
    private List<Map<String, Object>> trend;
    /** 设备状态分布 */
    private List<Map<String, Object>> distribution;

    public long getEquipmentTotal() { return equipmentTotal; }
    public void setEquipmentTotal(long equipmentTotal) { this.equipmentTotal = equipmentTotal; }
    public long getRunningCount() { return runningCount; }
    public void setRunningCount(long runningCount) { this.runningCount = runningCount; }
    public long getRepairCount() { return repairCount; }
    public void setRepairCount(long repairCount) { this.repairCount = repairCount; }
    public long getPendingOrderCount() { return pendingOrderCount; }
    public void setPendingOrderCount(long pendingOrderCount) { this.pendingOrderCount = pendingOrderCount; }
    public long getOverdueOrderCount() { return overdueOrderCount; }
    public void setOverdueOrderCount(long overdueOrderCount) { this.overdueOrderCount = overdueOrderCount; }
    public long getMonthCompletedCount() { return monthCompletedCount; }
    public void setMonthCompletedCount(long monthCompletedCount) { this.monthCompletedCount = monthCompletedCount; }
    public long getPlanDueCount() { return planDueCount; }
    public void setPlanDueCount(long planDueCount) { this.planDueCount = planDueCount; }
    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
    public List<Map<String, Object>> getTrend() { return trend; }
    public void setTrend(List<Map<String, Object>> trend) { this.trend = trend; }
    public List<Map<String, Object>> getDistribution() { return distribution; }
    public void setDistribution(List<Map<String, Object>> distribution) { this.distribution = distribution; }
}
