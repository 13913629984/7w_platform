package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.warehouse.WarehouseRequest;
import com.qws.common.dto.warehouse.WarehouseStatusRequest;
import com.qws.common.dto.warehouse.WarehouseVO;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    public List<WarehouseVO> list(String keyword, String type, Integer status) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Warehouse::getCode, keyword)
                    .or().like(Warehouse::getName, keyword)
                    .or().like(Warehouse::getManager, keyword)
                    .or().like(Warehouse::getAddress, keyword));
        }
        if (StringUtils.hasText(type)) wrapper.eq(Warehouse::getType, type);
        if (status != null) wrapper.eq(Warehouse::getStatus, status);
        wrapper.orderByAsc(Warehouse::getSort).orderByAsc(Warehouse::getId);
        return warehouseMapper.selectList(wrapper).stream().map(WarehouseVO::from).toList();
    }

    public WarehouseVO detail(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) throw new IllegalArgumentException("仓库不存在");
        return WarehouseVO.from(warehouse);
    }

    @Transactional
    public WarehouseVO create(WarehouseRequest request) {
        validate(request, false);
        if (warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getCode, request.getCode())) != null) {
            throw new IllegalStateException("仓库编码已存在");
        }
        Warehouse warehouse = new Warehouse();
        applyRequest(warehouse, request);
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());
        warehouseMapper.insert(warehouse);
        return detail(warehouse.getId());
    }

    @Transactional
    public WarehouseVO update(WarehouseRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("缺少仓库ID");
        Warehouse warehouse = warehouseMapper.selectById(request.getId());
        if (warehouse == null) throw new IllegalArgumentException("仓库不存在");
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(warehouse.getCode())) {
            if (warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>()
                    .eq(Warehouse::getCode, request.getCode()).ne(Warehouse::getId, request.getId())) != null) {
                throw new IllegalStateException("仓库编码已存在");
            }
        }
        applyRequest(warehouse, request);
        warehouse.setUpdateTime(LocalDateTime.now());
        warehouseMapper.updateById(warehouse);
        return detail(warehouse.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("缺少仓库ID");
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) throw new IllegalArgumentException("仓库不存在");
        if (warehouse.getUsedCapacity() != null && warehouse.getUsedCapacity().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("仓库已有容量占用，无法删除");
        }
        warehouseMapper.deleteById(id);
    }

    @Transactional
    public WarehouseVO changeStatus(WarehouseStatusRequest request) {
        if (request.getId() == null || request.getStatus() == null) throw new IllegalArgumentException("参数不完整");
        Warehouse warehouse = warehouseMapper.selectById(request.getId());
        if (warehouse == null) throw new IllegalArgumentException("仓库不存在");
        warehouse.setStatus(request.getStatus());
        warehouse.setUpdateTime(LocalDateTime.now());
        warehouseMapper.updateById(warehouse);
        return detail(warehouse.getId());
    }

    public Map<String, Object> stats() {
        List<Warehouse> list = warehouseMapper.selectList(null);
        BigDecimal totalCapacity = list.stream().map(w -> w.getCapacity() == null ? BigDecimal.ZERO : w.getCapacity()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal usedCapacity = list.stream().map(w -> w.getUsedCapacity() == null ? BigDecimal.ZERO : w.getUsedCapacity()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", list.size());
        stats.put("activeCount", list.stream().filter(w -> w.getStatus() != null && w.getStatus() == 1).count());
        stats.put("inactiveCount", list.stream().filter(w -> w.getStatus() != null && w.getStatus() == 0).count());
        stats.put("totalCapacity", totalCapacity);
        stats.put("usedCapacity", usedCapacity);
        stats.put("usageRate", totalCapacity.signum() > 0 ? usedCapacity.multiply(BigDecimal.valueOf(100)).divide(totalCapacity, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return stats;
    }

    private void validate(WarehouseRequest request, boolean isUpdate) {
        if (!isUpdate && !StringUtils.hasText(request.getCode())) throw new IllegalArgumentException("请填写仓库编码");
        if (StringUtils.hasText(request.getCode()) && request.getCode().length() > 50) throw new IllegalArgumentException("仓库编码长度不能超过50");
        if (!StringUtils.hasText(request.getName())) throw new IllegalArgumentException("请填写仓库名称");
        if (request.getName().length() > 100) throw new IllegalArgumentException("仓库名称长度不能超过100");
        if (!StringUtils.hasText(request.getType())) throw new IllegalArgumentException("请选择仓库类型");
        if (request.getCapacity() != null && request.getCapacity().signum() < 0) throw new IllegalArgumentException("容量不能小于0");
        if (request.getUsedCapacity() != null && request.getUsedCapacity().signum() < 0) throw new IllegalArgumentException("已用容量不能小于0");
        if (request.getCapacity() != null && request.getUsedCapacity() != null && request.getUsedCapacity().compareTo(request.getCapacity()) > 0) {
            throw new IllegalArgumentException("已用容量不能大于总容量");
        }
    }

    private void applyRequest(Warehouse warehouse, WarehouseRequest request) {
        if (StringUtils.hasText(request.getCode())) warehouse.setCode(request.getCode());
        warehouse.setName(request.getName());
        warehouse.setType(request.getType());
        warehouse.setAddress(request.getAddress());
        warehouse.setManager(request.getManager());
        warehouse.setPhone(request.getPhone());
        warehouse.setArea(request.getArea() == null ? BigDecimal.ZERO : request.getArea());
        warehouse.setCapacity(request.getCapacity() == null ? BigDecimal.ZERO : request.getCapacity());
        warehouse.setUsedCapacity(request.getUsedCapacity() == null ? BigDecimal.ZERO : request.getUsedCapacity());
        warehouse.setDescription(request.getDescription());
        warehouse.setSort(request.getSort() == null ? 0 : request.getSort());
        warehouse.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }
}