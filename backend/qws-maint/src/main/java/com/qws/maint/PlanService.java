package com.qws.maint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.maint.MaintListResult;
import com.qws.common.dto.maint.PlanRequest;
import com.qws.common.dto.maint.PlanVO;
import com.qws.common.entity.MaintEquipment;
import com.qws.common.entity.MaintPlan;
import com.qws.common.mapper.MaintEquipmentMapper;
import com.qws.common.mapper.MaintPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维保-保养计划服务：计划维护、周期推算执行、统计与下拉选项。
 */
@Service
public class PlanService {

    private static final List<String> CYCLE_TYPES = List.of("day", "week", "month", "quarter", "year");

    private final MaintPlanMapper planMapper;
    private final MaintEquipmentMapper equipmentMapper;

    public PlanService(MaintPlanMapper planMapper, MaintEquipmentMapper equipmentMapper) {
        this.planMapper = planMapper;
        this.equipmentMapper = equipmentMapper;
    }

    public MaintListResult<PlanVO> list(String keyword, String status, String cycleType,
                                        String equipmentKeyword, int page, int pageSize) {
        LambdaQueryWrapper<MaintPlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MaintPlan::getCode, keyword)
                    .or().like(MaintPlan::getName, keyword)
                    .or().like(MaintPlan::getEquipmentName, keyword)
                    .or().like(MaintPlan::getEquipmentCode, keyword)
                    .or().like(MaintPlan::getManager, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(MaintPlan::getStatus, status);
        }
        if (StringUtils.hasText(cycleType)) {
            wrapper.eq(MaintPlan::getCycleType, cycleType);
        }
        if (StringUtils.hasText(equipmentKeyword)) {
            wrapper.and(w -> w.like(MaintPlan::getEquipmentName, equipmentKeyword)
                    .or().like(MaintPlan::getEquipmentCode, equipmentKeyword));
        }
        wrapper.orderByAsc(MaintPlan::getNextDate).orderByDesc(MaintPlan::getId);
        Page<MaintPlan> result = planMapper.selectPage(
                new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);

        MaintListResult<PlanVO> payload = new MaintListResult<>();
        payload.setRows(result.getRecords().stream().map(PlanVO::from).toList());
        payload.setTotal(result.getTotal());
        payload.setPage(normalizePage(page));
        payload.setPageSize(normalizePageSize(pageSize));
        payload.setStats(stats());
        return payload;
    }

    public PlanVO detail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少计划ID");
        }
        MaintPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new IllegalArgumentException("保养计划不存在");
        }
        return PlanVO.from(plan);
    }

    @Transactional
    public PlanVO create(PlanRequest request) {
        validate(request, false);
        if (StringUtils.hasText(request.getCode()) && planMapper.selectCount(new LambdaQueryWrapper<MaintPlan>()
                .eq(MaintPlan::getCode, request.getCode())) > 0) {
            throw new IllegalArgumentException("计划编码已存在");
        }
        MaintPlan plan = new MaintPlan();
        applyRequest(plan, request);
        if (!StringUtils.hasText(plan.getCode())) {
            plan.setCode(generateCode());
        }
        plan.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
        plan.setExecuteCount(request.getExecuteCount() == null ? 0 : request.getExecuteCount());
        if (plan.getStartDate() == null) {
            plan.setStartDate(LocalDate.now());
        }
        if (plan.getNextDate() == null) {
            plan.setNextDate(computeNextDate(plan.getStartDate(), plan.getCycleType(), plan.getCycleValue()));
        }
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.insert(plan);
        return PlanVO.from(plan);
    }

    @Transactional
    public PlanVO update(PlanRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的保养计划");
        }
        MaintPlan plan = planMapper.selectById(request.getId());
        if (plan == null) {
            throw new IllegalArgumentException("保养计划不存在");
        }
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(plan.getCode())) {
            if (planMapper.selectCount(new LambdaQueryWrapper<MaintPlan>()
                    .eq(MaintPlan::getCode, request.getCode())
                    .ne(MaintPlan::getId, request.getId())) > 0) {
                throw new IllegalArgumentException("计划编码已存在");
            }
        }
        applyRequest(plan, request);
        if (StringUtils.hasText(request.getStatus())) {
            plan.setStatus(request.getStatus());
        }
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(plan);
        return PlanVO.from(plan);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少计划ID");
        }
        MaintPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new IllegalArgumentException("保养计划不存在");
        }
        planMapper.deleteById(id);
    }

    /**
     * 执行一次保养：记录执行日期、累加执行次数，并按周期推算下次保养日期。
     */
    @Transactional
    public PlanVO execute(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少计划ID");
        }
        MaintPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new IllegalArgumentException("保养计划不存在");
        }
        if ("finished".equals(plan.getStatus())) {
            throw new IllegalStateException("计划已完成，无法继续执行");
        }
        LocalDate today = LocalDate.now();
        plan.setLastExecuteDate(today);
        plan.setExecuteCount((plan.getExecuteCount() == null ? 0 : plan.getExecuteCount()) + 1);
        plan.setNextDate(computeNextDate(today, plan.getCycleType(), plan.getCycleValue()));
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(plan);
        // 同步回写设备最近保养日期
        if (plan.getEquipmentId() != null) {
            MaintEquipment equipment = equipmentMapper.selectById(plan.getEquipmentId());
            if (equipment != null) {
                equipment.setLastMaintainDate(today);
                equipment.setNextMaintainDate(plan.getNextDate());
                equipment.setUpdateTime(LocalDateTime.now());
                equipmentMapper.updateById(equipment);
            }
        }
        return PlanVO.from(plan);
    }

    /** 计划统计卡：总数、各状态数量、7 天内到期数、逾期数。 */
    public Map<String, Object> stats() {
        List<MaintPlan> plans = planMapper.selectList(null);
        LocalDate today = LocalDate.now();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", plans.size());
        stats.put("activeCount", plans.stream().filter(p -> "active".equals(p.getStatus())).count());
        stats.put("pausedCount", plans.stream().filter(p -> "paused".equals(p.getStatus())).count());
        stats.put("finishedCount", plans.stream().filter(p -> "finished".equals(p.getStatus())).count());
        stats.put("dueSoonCount", plans.stream().filter(p -> p.getNextDate() != null
                && !"finished".equals(p.getStatus())
                && !p.getNextDate().isAfter(today.plusDays(7))).count());
        stats.put("overdueCount", plans.stream().filter(p -> p.getNextDate() != null
                && !"finished".equals(p.getStatus())
                && p.getNextDate().isBefore(today)).count());
        stats.put("executeCountTotal", plans.stream()
                .mapToInt(p -> p.getExecuteCount() == null ? 0 : p.getExecuteCount()).sum());
        return stats;
    }

    /** 设备下拉选项。 */
    public List<Map<String, Object>> equipmentOptions() {
        return equipmentMapper.selectList(new LambdaQueryWrapper<MaintEquipment>()
                        .orderByAsc(MaintEquipment::getCode)).stream()
                .map(e -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("id", e.getId());
                    option.put("code", e.getCode());
                    option.put("name", e.getName());
                    option.put("customerName", e.getCustomerName());
                    return option;
                })
                .toList();
    }

    /** 周期推算：day/week/month/quarter/year。 */
    private LocalDate computeNextDate(LocalDate base, String cycleType, Integer cycleValue) {
        LocalDate from = base == null ? LocalDate.now() : base;
        int value = cycleValue == null || cycleValue <= 0 ? 1 : cycleValue;
        String type = StringUtils.hasText(cycleType) ? cycleType : "month";
        return switch (type) {
            case "day" -> from.plusDays(value);
            case "week" -> from.plusDays(7L * value);
            case "month" -> from.plusMonths(value);
            case "quarter" -> from.plusMonths(3L * value);
            case "year" -> from.plusYears(value);
            default -> throw new IllegalArgumentException("未知的周期类型：" + type);
        };
    }

    private String generateCode() {
        return "MP" + LocalDate.now().toString().replace("-", "")
                + String.format("%03d", planMapper.selectCount(null) + 1);
    }

    private void validate(PlanRequest request, boolean isUpdate) {
        if (request == null) {
            throw new IllegalArgumentException("请求数据不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写计划名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("计划名称长度不能超过100");
        }
        if (StringUtils.hasText(request.getCycleType()) && !CYCLE_TYPES.contains(request.getCycleType())) {
            throw new IllegalArgumentException("未知的周期类型：" + request.getCycleType());
        }
        if (request.getCycleValue() != null && request.getCycleValue() <= 0) {
            throw new IllegalArgumentException("周期数值必须大于0");
        }
    }

    private void applyRequest(MaintPlan plan, PlanRequest request) {
        if (StringUtils.hasText(request.getCode())) plan.setCode(request.getCode());
        plan.setName(request.getName());
        plan.setCycleType(StringUtils.hasText(request.getCycleType()) ? request.getCycleType() : "month");
        plan.setCycleValue(request.getCycleValue() == null || request.getCycleValue() <= 0 ? 1 : request.getCycleValue());
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        if (request.getNextDate() != null) {
            plan.setNextDate(request.getNextDate());
        }
        plan.setItems(request.getItems());
        plan.setManager(request.getManager());
        plan.setRemark(request.getRemark());
        plan.setLastExecuteDate(request.getLastExecuteDate());
        if (request.getExecuteCount() != null) {
            plan.setExecuteCount(request.getExecuteCount());
        }
        // 关联设备时自动回填设备编码/名称
        if (request.getEquipmentId() != null) {
            MaintEquipment equipment = equipmentMapper.selectById(request.getEquipmentId());
            if (equipment == null) {
                throw new IllegalArgumentException("关联设备不存在");
            }
            plan.setEquipmentId(equipment.getId());
            plan.setEquipmentCode(equipment.getCode());
            plan.setEquipmentName(equipment.getName());
        }
    }

    private int normalizePage(int page) {
        return page <= 0 ? 1 : page;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }
}
