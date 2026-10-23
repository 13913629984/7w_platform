package com.qws.maint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.maint.EquipmentRequest;
import com.qws.common.dto.maint.EquipmentVO;
import com.qws.common.dto.maint.MaintListResult;
import com.qws.common.entity.MaintEquipment;
import com.qws.common.mapper.MaintEquipmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维保-设备台账服务：设备档案的增删改查、统计与下拉选项。
 */
@Service
public class EquipmentService {

    private final MaintEquipmentMapper equipmentMapper;

    public EquipmentService(MaintEquipmentMapper equipmentMapper) {
        this.equipmentMapper = equipmentMapper;
    }

    /** 设备分页列表（含统计卡数据）。 */
    public MaintListResult<EquipmentVO> list(String keyword, String status, String category,
                                             String customerName, int page, int pageSize) {
        LambdaQueryWrapper<MaintEquipment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MaintEquipment::getCode, keyword)
                    .or().like(MaintEquipment::getName, keyword)
                    .or().like(MaintEquipment::getModel, keyword)
                    .or().like(MaintEquipment::getBrand, keyword)
                    .or().like(MaintEquipment::getCustomerName, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(MaintEquipment::getStatus, status);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(MaintEquipment::getCategory, category);
        }
        if (StringUtils.hasText(customerName)) {
            wrapper.like(MaintEquipment::getCustomerName, customerName);
        }
        wrapper.orderByDesc(MaintEquipment::getId);
        Page<MaintEquipment> result = equipmentMapper.selectPage(new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);

        MaintListResult<EquipmentVO> payload = new MaintListResult<>();
        payload.setRows(result.getRecords().stream().map(EquipmentVO::from).toList());
        payload.setTotal(result.getTotal());
        payload.setPage(normalizePage(page));
        payload.setPageSize(normalizePageSize(pageSize));
        payload.setStats(stats());
        return payload;
    }

    public EquipmentVO detail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少设备ID");
        }
        MaintEquipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        return EquipmentVO.from(equipment);
    }

    @Transactional
    public EquipmentVO create(EquipmentRequest request) {
        validate(request, false);
        if (equipmentMapper.selectCount(new LambdaQueryWrapper<MaintEquipment>()
                .eq(MaintEquipment::getCode, request.getCode())) > 0) {
            throw new IllegalArgumentException("设备编码已存在");
        }
        MaintEquipment equipment = new MaintEquipment();
        applyRequest(equipment, request);
        if (equipment.getNextMaintainDate() == null && equipment.getWarrantyEnd() != null) {
            // 未指定下次保养日期时，默认按保修到期日推算（不早于今天）
            LocalDate today = LocalDate.now();
            equipment.setNextMaintainDate(equipment.getWarrantyEnd().isBefore(today) ? today : equipment.getWarrantyEnd());
        }
        equipment.setCreateTime(LocalDateTime.now());
        equipment.setUpdateTime(LocalDateTime.now());
        equipmentMapper.insert(equipment);
        return EquipmentVO.from(equipment);
    }

    @Transactional
    public EquipmentVO update(EquipmentRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的设备");
        }
        MaintEquipment equipment = equipmentMapper.selectById(request.getId());
        if (equipment == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(equipment.getCode())) {
            if (equipmentMapper.selectCount(new LambdaQueryWrapper<MaintEquipment>()
                    .eq(MaintEquipment::getCode, request.getCode())
                    .ne(MaintEquipment::getId, request.getId())) > 0) {
                throw new IllegalArgumentException("设备编码已存在");
            }
        }
        applyRequest(equipment, request);
        equipment.setUpdateTime(LocalDateTime.now());
        equipmentMapper.updateById(equipment);
        return EquipmentVO.from(equipment);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少设备ID");
        }
        MaintEquipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        equipmentMapper.deleteById(id);
    }

    /** 设备统计卡：总数、各状态数量、保修将到期数、保养将到期数。 */
    public Map<String, Object> stats() {
        List<MaintEquipment> equipments = equipmentMapper.selectList(null);
        LocalDate today = LocalDate.now();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", equipments.size());
        stats.put("runningCount", countStatus(equipments, "running"));
        stats.put("idleCount", countStatus(equipments, "idle"));
        stats.put("repairCount", countStatus(equipments, "repair"));
        stats.put("scrappedCount", countStatus(equipments, "scrapped"));
        stats.put("warrantyExpiringCount", equipments.stream()
                .filter(e -> e.getWarrantyEnd() != null
                        && !e.getWarrantyEnd().isBefore(today)
                        && !e.getWarrantyEnd().isAfter(today.plusDays(30)))
                .count());
        stats.put("maintainDueCount", equipments.stream()
                .filter(e -> e.getNextMaintainDate() != null
                        && !e.getNextMaintainDate().isAfter(today.plusDays(7)))
                .count());
        return stats;
    }

    /** 设备下拉选项（供工单、保养计划表单使用）。 */
    public List<Map<String, Object>> options() {
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

    /** 按类别汇总（工作台可用）。 */
    public Map<String, Long> categoryCounts() {
        Map<String, Long> counts = new HashMap<>();
        equipmentMapper.selectList(null).forEach(e -> {
            String category = StringUtils.hasText(e.getCategory()) ? e.getCategory() : "其他";
            counts.merge(category, 1L, Long::sum);
        });
        return counts;
    }

    private long countStatus(List<MaintEquipment> list, String status) {
        return list.stream().filter(e -> status.equals(e.getStatus())).count();
    }

    private void validate(EquipmentRequest request, boolean isUpdate) {
        if (request == null) {
            throw new IllegalArgumentException("请求数据不能为空");
        }
        if (!isUpdate && !StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("请填写设备编码");
        }
        if (StringUtils.hasText(request.getCode()) && request.getCode().length() > 50) {
            throw new IllegalArgumentException("设备编码长度不能超过50");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写设备名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("设备名称长度不能超过100");
        }
    }

    private void applyRequest(MaintEquipment equipment, EquipmentRequest request) {
        if (StringUtils.hasText(request.getCode())) equipment.setCode(request.getCode());
        equipment.setName(request.getName());
        equipment.setModel(request.getModel());
        equipment.setBrand(request.getBrand());
        equipment.setCategory(request.getCategory());
        equipment.setCustomerId(request.getCustomerId());
        equipment.setCustomerName(request.getCustomerName());
        equipment.setLocation(request.getLocation());
        equipment.setInstallDate(request.getInstallDate());
        equipment.setWarrantyEnd(request.getWarrantyEnd());
        equipment.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "running");
        equipment.setManager(request.getManager());
        equipment.setPhone(request.getPhone());
        equipment.setPurchasePrice(request.getPurchasePrice());
        equipment.setLastMaintainDate(request.getLastMaintainDate());
        equipment.setNextMaintainDate(request.getNextMaintainDate() != null
                ? request.getNextMaintainDate() : equipment.getNextMaintainDate());
        equipment.setRemark(request.getRemark());
    }

    private int normalizePage(int page) {
        return page <= 0 ? 1 : page;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }
}
