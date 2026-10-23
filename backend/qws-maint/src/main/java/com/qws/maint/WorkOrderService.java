package com.qws.maint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.maint.MaintListResult;
import com.qws.common.dto.maint.WorkOrderRequest;
import com.qws.common.dto.maint.WorkOrderStatusRequest;
import com.qws.common.dto.maint.WorkOrderVO;
import com.qws.common.entity.MaintEquipment;
import com.qws.common.entity.MaintWorkOrder;
import com.qws.common.mapper.MaintEquipmentMapper;
import com.qws.common.mapper.MaintWorkOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维保-维修工单服务：工单派发、状态流转、统计与下拉选项。
 */
@Service
public class WorkOrderService {

    private static final DateTimeFormatter CODE_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<String> STATUS_FLOW =
            List.of("pending", "assigned", "processing", "completed", "closed");
    /** 处理人候选（暂无用户中心对接，使用维保班组固定名单） */
    private static final List<String> HANDLERS =
            List.of("张伟", "李强", "王敏", "赵磊", "陈刚", "周涛");

    private final MaintWorkOrderMapper workOrderMapper;
    private final MaintEquipmentMapper equipmentMapper;

    public WorkOrderService(MaintWorkOrderMapper workOrderMapper, MaintEquipmentMapper equipmentMapper) {
        this.workOrderMapper = workOrderMapper;
        this.equipmentMapper = equipmentMapper;
    }

    public MaintListResult<WorkOrderVO> list(String keyword, String status, String priority,
                                             String type, int page, int pageSize) {
        LambdaQueryWrapper<MaintWorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MaintWorkOrder::getCode, keyword)
                    .or().like(MaintWorkOrder::getTitle, keyword)
                    .or().like(MaintWorkOrder::getEquipmentName, keyword)
                    .or().like(MaintWorkOrder::getEquipmentCode, keyword)
                    .or().like(MaintWorkOrder::getCustomerName, keyword)
                    .or().like(MaintWorkOrder::getHandler, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(MaintWorkOrder::getStatus, status);
        }
        if (StringUtils.hasText(priority)) {
            wrapper.eq(MaintWorkOrder::getPriority, priority);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(MaintWorkOrder::getType, type);
        }
        wrapper.orderByDesc(MaintWorkOrder::getReportTime).orderByDesc(MaintWorkOrder::getId);
        Page<MaintWorkOrder> result = workOrderMapper.selectPage(
                new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);

        MaintListResult<WorkOrderVO> payload = new MaintListResult<>();
        payload.setRows(result.getRecords().stream().map(WorkOrderVO::from).toList());
        payload.setTotal(result.getTotal());
        payload.setPage(normalizePage(page));
        payload.setPageSize(normalizePageSize(pageSize));
        payload.setStats(stats());
        return payload;
    }

    public WorkOrderVO detail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少工单ID");
        }
        MaintWorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        return WorkOrderVO.from(order);
    }

    @Transactional
    public WorkOrderVO create(WorkOrderRequest request) {
        validate(request);
        MaintWorkOrder order = new MaintWorkOrder();
        applyRequest(order, request);
        order.setCode(generateCode());
        order.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "pending");
        if (order.getReportTime() == null) {
            order.setReportTime(LocalDateTime.now());
        }
        if ("completed".equals(order.getStatus()) || "closed".equals(order.getStatus())) {
            order.setFinishTime(order.getFinishTime() == null ? LocalDateTime.now() : order.getFinishTime());
        }
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        workOrderMapper.insert(order);
        // 实体 MaintWorkOrder 未提供 faultDesc 的 setter（契约要求不改既有实体），故用 UpdateWrapper 单独写入
        syncFaultDesc(order.getId(), request.getFaultDesc());
        return detail(order.getId());
    }

    @Transactional
    public WorkOrderVO update(WorkOrderRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的工单");
        }
        MaintWorkOrder order = workOrderMapper.selectById(request.getId());
        if (order == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        validate(request);
        applyRequest(order, request);
        if (StringUtils.hasText(request.getStatus())) {
            order.setStatus(request.getStatus());
            if ("completed".equals(request.getStatus()) || "closed".equals(request.getStatus())) {
                if (order.getFinishTime() == null) {
                    order.setFinishTime(LocalDateTime.now());
                }
            }
        }
        order.setUpdateTime(LocalDateTime.now());
        workOrderMapper.updateById(order);
        syncFaultDesc(order.getId(), request.getFaultDesc());
        return detail(order.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少工单ID");
        }
        MaintWorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        workOrderMapper.deleteById(id);
    }

    /**
     * 工单状态流转：pending → assigned → processing → completed → closed。
     * 不允许跳跃或回退（已完成工单可关闭）。
     */
    @Transactional
    public WorkOrderVO changeStatus(WorkOrderStatusRequest request) {
        if (request == null || request.getId() == null || !StringUtils.hasText(request.getStatus())) {
            throw new IllegalArgumentException("参数不完整");
        }
        MaintWorkOrder order = workOrderMapper.selectById(request.getId());
        if (order == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        String current = StringUtils.hasText(order.getStatus()) ? order.getStatus() : "pending";
        String target = request.getStatus();
        int currentIndex = STATUS_FLOW.indexOf(current);
        int targetIndex = STATUS_FLOW.indexOf(target);
        if (targetIndex < 0) {
            throw new IllegalArgumentException("未知的工单状态：" + target);
        }
        if (currentIndex < 0) {
            throw new IllegalArgumentException("工单当前状态异常：" + current);
        }
        if (targetIndex != currentIndex + 1) {
            throw new IllegalArgumentException("工单状态不允许从 " + current + " 变更为 " + target);
        }
        if ("closed".equals(current)) {
            throw new IllegalArgumentException("工单已归档，无法变更状态");
        }
        order.setStatus(target);
        if (StringUtils.hasText(request.getHandler())) {
            order.setHandler(request.getHandler());
        }
        if ("assigned".equals(target) && !StringUtils.hasText(order.getHandler())) {
            throw new IllegalArgumentException("派单前请先指定处理人");
        }
        if ("completed".equals(target)) {
            order.setFinishTime(LocalDateTime.now());
        }
        if (StringUtils.hasText(request.getRemark())) {
            order.setRemark(request.getRemark());
        }
        order.setUpdateTime(LocalDateTime.now());
        workOrderMapper.updateById(order);
        return WorkOrderVO.from(order);
    }

    /** 工单统计卡：总数、各状态数量、逾期数、本月完成数。 */
    public Map<String, Object> stats() {
        List<MaintWorkOrder> orders = workOrderMapper.selectList(null);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", orders.size());
        stats.put("pendingCount", countStatus(orders, "pending"));
        stats.put("assignedCount", countStatus(orders, "assigned"));
        stats.put("processingCount", countStatus(orders, "processing"));
        stats.put("completedCount", countStatus(orders, "completed"));
        stats.put("closedCount", countStatus(orders, "closed"));
        stats.put("urgentCount", orders.stream().filter(o -> "urgent".equals(o.getPriority())
                && !isFinished(o)).count());
        stats.put("overdueCount", orders.stream().filter(o -> o.getPlanTime() != null
                && o.getPlanTime().isBefore(now) && !isFinished(o)).count());
        stats.put("monthCompletedCount", orders.stream().filter(o -> o.getFinishTime() != null
                && !o.getFinishTime().toLocalDate().isBefore(today.withDayOfMonth(1))).count());
        return stats;
    }

    /** 设备下拉选项（复用设备台账）。 */
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

    /** 处理人下拉选项。 */
    public List<Map<String, Object>> handlerOptions() {
        return HANDLERS.stream().map(name -> {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("value", name);
            option.put("label", name);
            return option;
        }).toList();
    }

    /** 生成工单号：WO + yyyyMMdd + 3 位当日序号。 */
    private String generateCode() {
        String prefix = "WO" + LocalDate.now().format(CODE_DAY);
        MaintWorkOrder latest = workOrderMapper.selectOne(new LambdaQueryWrapper<MaintWorkOrder>()
                .likeRight(MaintWorkOrder::getCode, prefix)
                .orderByDesc(MaintWorkOrder::getCode)
                .last("LIMIT 1"));
        int seq = 1;
        if (latest != null && StringUtils.hasText(latest.getCode())) {
            String tail = latest.getCode().substring(prefix.length());
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    /** 故障描述单独回写（实体缺少 faultDesc setter）。 */
    private void syncFaultDesc(Long id, String faultDesc) {
        if (id == null || !StringUtils.hasText(faultDesc)) {
            return;
        }
        workOrderMapper.update(null, new LambdaUpdateWrapper<MaintWorkOrder>()
                .eq(MaintWorkOrder::getId, id)
                .set(MaintWorkOrder::getFaultDesc, faultDesc));
    }

    private boolean isFinished(MaintWorkOrder order) {
        return "completed".equals(order.getStatus()) || "closed".equals(order.getStatus());
    }

    private long countStatus(List<MaintWorkOrder> list, String status) {
        return list.stream().filter(o -> status.equals(o.getStatus())).count();
    }

    private void validate(WorkOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求数据不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("请填写工单标题");
        }
        if (request.getTitle().length() > 200) {
            throw new IllegalArgumentException("工单标题长度不能超过200");
        }
        if (StringUtils.hasText(request.getStatus()) && !STATUS_FLOW.contains(request.getStatus())) {
            throw new IllegalArgumentException("未知的工单状态：" + request.getStatus());
        }
    }

    private void applyRequest(MaintWorkOrder order, WorkOrderRequest request) {
        order.setTitle(request.getTitle());
        order.setType(StringUtils.hasText(request.getType()) ? request.getType() : "repair");
        order.setPriority(StringUtils.hasText(request.getPriority()) ? request.getPriority() : "normal");
        order.setReporter(request.getReporter());
        order.setHandler(request.getHandler());
        order.setReportTime(request.getReportTime() != null ? request.getReportTime() : order.getReportTime());
        order.setPlanTime(request.getPlanTime());
        order.setFinishTime(request.getFinishTime());
        order.setHours(request.getHours());
        order.setCost(request.getCost());
        order.setSolution(request.getSolution());
        order.setRemark(request.getRemark());
        // 关联设备时自动回填设备编码/名称，客户名未填时取设备客户
        if (request.getEquipmentId() != null) {
            MaintEquipment equipment = equipmentMapper.selectById(request.getEquipmentId());
            if (equipment == null) {
                throw new IllegalArgumentException("关联设备不存在");
            }
            order.setEquipmentId(equipment.getId());
            order.setEquipmentCode(equipment.getCode());
            order.setEquipmentName(equipment.getName());
            order.setCustomerName(StringUtils.hasText(request.getCustomerName())
                    ? request.getCustomerName() : equipment.getCustomerName());
        } else {
            order.setCustomerName(request.getCustomerName());
        }
    }

    private int normalizePage(int page) {
        return page <= 0 ? 1 : page;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }
}
