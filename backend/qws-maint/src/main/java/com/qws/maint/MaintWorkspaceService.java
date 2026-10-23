package com.qws.maint;

import com.qws.common.dto.maint.MaintOverviewVO;
import com.qws.common.entity.MaintEquipment;
import com.qws.common.entity.MaintPlan;
import com.qws.common.entity.MaintSparePart;
import com.qws.common.entity.MaintWorkOrder;
import com.qws.common.mapper.MaintEquipmentMapper;
import com.qws.common.mapper.MaintPlanMapper;
import com.qws.common.mapper.MaintSparePartMapper;
import com.qws.common.mapper.MaintWorkOrderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维保工作台聚合服务：设备/工单/计划/备件关键指标、近 7 日工单趋势、设备状态分布。
 */
@Service
public class MaintWorkspaceService {

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M/d");

    /** 设备状态中文标签映射 */
    private static final Map<String, String> EQUIPMENT_STATUS_LABELS = new LinkedHashMap<>();

    static {
        EQUIPMENT_STATUS_LABELS.put("running", "运行中");
        EQUIPMENT_STATUS_LABELS.put("idle", "闲置");
        EQUIPMENT_STATUS_LABELS.put("repair", "维修中");
        EQUIPMENT_STATUS_LABELS.put("scrapped", "已报废");
    }

    private final MaintEquipmentMapper equipmentMapper;
    private final MaintWorkOrderMapper workOrderMapper;
    private final MaintPlanMapper planMapper;
    private final MaintSparePartMapper sparePartMapper;

    public MaintWorkspaceService(MaintEquipmentMapper equipmentMapper, MaintWorkOrderMapper workOrderMapper,
                                 MaintPlanMapper planMapper, MaintSparePartMapper sparePartMapper) {
        this.equipmentMapper = equipmentMapper;
        this.workOrderMapper = workOrderMapper;
        this.planMapper = planMapper;
        this.sparePartMapper = sparePartMapper;
    }

    /** 工作台汇总。 */
    public MaintOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<MaintEquipment> equipments = equipmentMapper.selectList(null);
        List<MaintWorkOrder> orders = workOrderMapper.selectList(null);
        List<MaintPlan> plans = planMapper.selectList(null);
        List<MaintSparePart> spareParts = sparePartMapper.selectList(null);

        MaintOverviewVO vo = new MaintOverviewVO();
        vo.setEquipmentTotal(equipments.size());
        vo.setRunningCount(equipments.stream().filter(e -> "running".equals(e.getStatus())).count());
        vo.setRepairCount(equipments.stream().filter(e -> "repair".equals(e.getStatus())).count());
        vo.setPendingOrderCount(orders.stream()
                .filter(o -> "pending".equals(o.getStatus()) || "assigned".equals(o.getStatus())).count());
        vo.setOverdueOrderCount(orders.stream()
                .filter(o -> o.getPlanTime() != null && o.getPlanTime().isBefore(now) && !isFinished(o)).count());
        vo.setMonthCompletedCount(orders.stream()
                .filter(o -> o.getFinishTime() != null
                        && !o.getFinishTime().toLocalDate().isBefore(today.withDayOfMonth(1))).count());
        vo.setPlanDueCount(plans.stream()
                .filter(p -> p.getNextDate() != null && !"finished".equals(p.getStatus())
                        && !p.getNextDate().isAfter(today.plusDays(7))).count());
        vo.setLowStockCount(spareParts.stream()
                .filter(p -> p.getStockQty() != null && p.getSafetyQty() != null
                        && p.getStockQty().compareTo(p.getSafetyQty()) < 0).count());
        vo.setTrend(trend());
        vo.setDistribution(distribution());
        return vo;
    }

    /** 近 7 日工单量趋势（按报修时间统计，含创建/完成数量）。 */
    public List<Map<String, Object>> trend() {
        LocalDate today = LocalDate.now();
        List<MaintWorkOrder> orders = workOrderMapper.selectList(null);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = day.format(DAY_KEY);
            long created = orders.stream()
                    .filter(o -> o.getReportTime() != null && key.equals(o.getReportTime().toLocalDate().format(DAY_KEY)))
                    .count();
            long finished = orders.stream()
                    .filter(o -> o.getFinishTime() != null && key.equals(o.getFinishTime().toLocalDate().format(DAY_KEY)))
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", key);
            point.put("label", day.format(DAY_LABEL));
            point.put("created", created);
            point.put("finished", finished);
            trend.add(point);
        }
        return trend;
    }

    /** 设备状态分布。 */
    public List<Map<String, Object>> distribution() {
        List<MaintEquipment> equipments = equipmentMapper.selectList(null);
        List<Map<String, Object>> distribution = new ArrayList<>();
        EQUIPMENT_STATUS_LABELS.forEach((status, label) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("label", label);
            item.put("count", equipments.stream().filter(e -> status.equals(e.getStatus())).count());
            distribution.add(item);
        });
        return distribution;
    }

    private boolean isFinished(MaintWorkOrder order) {
        return "completed".equals(order.getStatus()) || "closed".equals(order.getStatus());
    }
}
