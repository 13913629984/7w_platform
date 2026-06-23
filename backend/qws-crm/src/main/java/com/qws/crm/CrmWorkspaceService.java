package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.workspace.WorkspaceActivityVO;
import com.qws.common.dto.workspace.WorkspaceLeadVO;
import com.qws.common.dto.workspace.WorkspaceOverview;
import com.qws.common.entity.CrmActivity;
import com.qws.common.entity.CrmCustomer;
import com.qws.common.entity.CrmLead;
import com.qws.common.entity.CrmOrder;
import com.qws.common.mapper.CrmActivityMapper;
import com.qws.common.mapper.CrmCustomerMapper;
import com.qws.common.mapper.CrmDealMapper;
import com.qws.common.mapper.CrmLeadMapper;
import com.qws.common.mapper.CrmOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CrmWorkspaceService {

    private static final List<String> CLOSED_DEAL_STATUS = List.of("已转订单");

    private final CrmCustomerMapper customerMapper;
    private final CrmDealMapper dealMapper;
    private final CrmOrderMapper orderMapper;
    private final CrmLeadMapper leadMapper;
    private final CrmActivityMapper activityMapper;

    public CrmWorkspaceService(CrmCustomerMapper customerMapper,
                               CrmDealMapper dealMapper,
                               CrmOrderMapper orderMapper,
                               CrmLeadMapper leadMapper,
                               CrmActivityMapper activityMapper) {
        this.customerMapper = customerMapper;
        this.dealMapper = dealMapper;
        this.orderMapper = orderMapper;
        this.leadMapper = leadMapper;
        this.activityMapper = activityMapper;
    }

    public WorkspaceOverview overview() {
        WorkspaceOverview overview = new WorkspaceOverview();
        overview.setStats(computeStats());
        overview.setLeads(pendingLeads());
        overview.setActivities(upcomingActivities());
        overview.setSalesTrend(salesTrend());
        overview.setCustomerDistribution(customerDistribution());
        return overview;
    }

    private Map<String, Object> computeStats() {
        Map<String, Object> stats = new HashMap<>();
        List<CrmCustomer> customers = customerMapper.selectList(null);
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);

        int customerTotal = customers.size();
        long monthAddedCustomers = customers.stream()
                .filter(c -> c.getCreateTime() != null && !c.getCreateTime().toLocalDate().isBefore(firstDayOfMonth))
                .count();

        // 待跟进商机：未转订单的进行中商机
        long pendingDeals = dealMapper.selectList(null).stream()
                .filter(d -> !CLOSED_DEAL_STATUS.contains(d.getStatus()))
                .count();

        // 本月成交：本月签订的订单金额合计（单位：万）
        BigDecimal monthDealAmount = orderMapper.selectList(null).stream()
                .filter(o -> o.getSignDate() != null && !o.getSignDate().isBefore(firstDayOfMonth))
                .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("customerTotal", customerTotal);
        stats.put("monthAddedCustomers", monthAddedCustomers);
        stats.put("pendingDeals", pendingDeals);
        stats.put("monthDealAmount", monthDealAmount.stripTrailingZeros().toPlainString());
        return stats;
    }

    private List<WorkspaceLeadVO> pendingLeads() {
        return leadMapper.selectList(new LambdaQueryWrapper<CrmLead>()
                        .eq(CrmLead::getStatus, "待跟进")
                        .orderByAsc(CrmLead::getFollowAt)).stream()
                .map(WorkspaceLeadVO::from)
                .collect(Collectors.toList());
    }

    private List<WorkspaceActivityVO> upcomingActivities() {
        return activityMapper.selectList(new LambdaQueryWrapper<CrmActivity>()
                        .eq(CrmActivity::getStatus, "待处理")
                        .orderByAsc(CrmActivity::getActivityAt)).stream()
                .map(WorkspaceActivityVO::from)
                .collect(Collectors.toList());
    }

    // 销售趋势：近6个月按订单签订金额汇总（单位：万）
    private List<Map<String, Object>> salesTrend() {
        List<CrmOrder> orders = orderMapper.selectList(null);
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        for (int i = 0; i < 6; i++) {
            byMonth.put(String.format("%02d月", cursor.getMonthValue()), BigDecimal.ZERO);
            cursor = cursor.plusMonths(1);
        }
        LocalDate windowStart = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        for (CrmOrder order : orders) {
            if (order.getSignDate() == null || order.getSignDate().isBefore(windowStart)) {
                continue;
            }
            String key = String.format("%02d月", order.getSignDate().getMonthValue());
            BigDecimal amount = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
            byMonth.merge(key, amount, BigDecimal::add);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : byMonth.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", entry.getKey());
            point.put("amount", entry.getValue().stripTrailingZeros().toPlainString());
            trend.add(point);
        }
        return trend;
    }

    // 客户分布：按客户等级分组统计
    private List<Map<String, Object>> customerDistribution() {
        List<CrmCustomer> customers = customerMapper.selectList(null);
        Map<String, Long> byLevel = customers.stream()
                .filter(c -> c.getLevel() != null)
                .collect(Collectors.groupingBy(CrmCustomer::getLevel, Collectors.counting()));
        List<Map<String, Object>> distribution = new ArrayList<>();
        byLevel.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("level", entry.getKey());
                    point.put("count", entry.getValue());
                    distribution.add(point);
                });
        return distribution;
    }
}
