package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.location.LocationRequest;
import com.qws.common.dto.location.LocationStatusRequest;
import com.qws.common.dto.location.LocationVO;
import com.qws.common.entity.Location;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.LocationMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationMapper locationMapper;
    private final WarehouseMapper warehouseMapper;

    public LocationService(LocationMapper locationMapper, WarehouseMapper warehouseMapper) {
        this.locationMapper = locationMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public List<LocationVO> list(String keyword, String warehouseCode, String type, Integer status) {
        LambdaQueryWrapper<Location> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Location::getCode, keyword)
                    .or().like(Location::getName, keyword)
                    .or().like(Location::getArea, keyword)
                    .or().like(Location::getShelf, keyword));
        }
        if (StringUtils.hasText(warehouseCode)) wrapper.eq(Location::getWarehouseCode, warehouseCode);
        if (StringUtils.hasText(type)) wrapper.eq(Location::getType, type);
        if (status != null) wrapper.eq(Location::getStatus, status);
        wrapper.orderByAsc(Location::getWarehouseCode).orderByAsc(Location::getSort).orderByAsc(Location::getId);
        Map<String, String> warehouseNames = warehouseNameMap();
        return locationMapper.selectList(wrapper).stream().map(location -> {
            LocationVO vo = LocationVO.from(location);
            vo.setWarehouseName(warehouseNames.get(location.getWarehouseCode()));
            return vo;
        }).toList();
    }

    public LocationVO detail(Long id) {
        Location location = locationMapper.selectById(id);
        if (location == null) throw new IllegalArgumentException("库位不存在");
        LocationVO vo = LocationVO.from(location);
        vo.setWarehouseName(warehouseNameMap().get(location.getWarehouseCode()));
        return vo;
    }

    @Transactional
    public LocationVO create(LocationRequest request) {
        validate(request, false);
        if (locationMapper.selectOne(new LambdaQueryWrapper<Location>().eq(Location::getCode, request.getCode())) != null) {
            throw new IllegalStateException("库位编码已存在");
        }
        Location location = new Location();
        applyRequest(location, request);
        location.setCreateTime(LocalDateTime.now());
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.insert(location);
        return detail(location.getId());
    }

    @Transactional
    public LocationVO update(LocationRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("缺少库位ID");
        Location location = locationMapper.selectById(request.getId());
        if (location == null) throw new IllegalArgumentException("库位不存在");
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(location.getCode())) {
            if (locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                    .eq(Location::getCode, request.getCode()).ne(Location::getId, request.getId())) != null) {
                throw new IllegalStateException("库位编码已存在");
            }
        }
        applyRequest(location, request);
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.updateById(location);
        return detail(location.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("缺少库位ID");
        Location location = locationMapper.selectById(id);
        if (location == null) throw new IllegalArgumentException("库位不存在");
        if (location.getUsedCapacity() != null && location.getUsedCapacity().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("库位已有容量占用，无法删除");
        }
        locationMapper.deleteById(id);
    }

    @Transactional
    public LocationVO changeStatus(LocationStatusRequest request) {
        if (request.getId() == null || request.getStatus() == null) throw new IllegalArgumentException("参数不完整");
        Location location = locationMapper.selectById(request.getId());
        if (location == null) throw new IllegalArgumentException("库位不存在");
        location.setStatus(request.getStatus());
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.updateById(location);
        return detail(location.getId());
    }

    public Map<String, Object> stats() {
        List<Location> list = locationMapper.selectList(null);
        BigDecimal totalCapacity = list.stream().map(l -> l.getCapacity() == null ? BigDecimal.ZERO : l.getCapacity()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal usedCapacity = list.stream().map(l -> l.getUsedCapacity() == null ? BigDecimal.ZERO : l.getUsedCapacity()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", list.size());
        stats.put("activeCount", list.stream().filter(l -> l.getStatus() != null && l.getStatus() == 1).count());
        stats.put("inactiveCount", list.stream().filter(l -> l.getStatus() != null && l.getStatus() == 0).count());
        stats.put("occupiedCount", list.stream().filter(l -> l.getUsedCapacity() != null && l.getUsedCapacity().compareTo(BigDecimal.ZERO) > 0).count());
        stats.put("emptyCount", list.stream().filter(l -> l.getUsedCapacity() == null || l.getUsedCapacity().compareTo(BigDecimal.ZERO) == 0).count());
        stats.put("totalCapacity", totalCapacity);
        stats.put("usedCapacity", usedCapacity);
        stats.put("usageRate", totalCapacity.signum() > 0 ? usedCapacity.multiply(BigDecimal.valueOf(100)).divide(totalCapacity, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return stats;
    }

    private void validate(LocationRequest request, boolean isUpdate) {
        if (!isUpdate && !StringUtils.hasText(request.getCode())) throw new IllegalArgumentException("请填写库位编码");
        if (StringUtils.hasText(request.getCode()) && request.getCode().length() > 50) throw new IllegalArgumentException("库位编码长度不能超过50");
        if (!StringUtils.hasText(request.getName())) throw new IllegalArgumentException("请填写库位名称");
        if (!StringUtils.hasText(request.getWarehouseCode())) throw new IllegalArgumentException("请选择所属仓库");
        Warehouse warehouse = warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getCode, request.getWarehouseCode()));
        if (warehouse == null) throw new IllegalArgumentException("所属仓库不存在");
        if (!StringUtils.hasText(request.getType())) throw new IllegalArgumentException("请选择库位类型");
        if (request.getCapacity() != null && request.getCapacity().signum() < 0) throw new IllegalArgumentException("容量不能小于0");
        if (request.getUsedCapacity() != null && request.getUsedCapacity().signum() < 0) throw new IllegalArgumentException("已用容量不能小于0");
        if (request.getCapacity() != null && request.getUsedCapacity() != null && request.getUsedCapacity().compareTo(request.getCapacity()) > 0) {
            throw new IllegalArgumentException("已用容量不能大于总容量");
        }
    }

    private void applyRequest(Location location, LocationRequest request) {
        if (StringUtils.hasText(request.getCode())) location.setCode(request.getCode());
        location.setName(request.getName());
        location.setWarehouseCode(request.getWarehouseCode());
        location.setArea(request.getArea());
        location.setShelf(request.getShelf());
        location.setLayer(request.getLayer());
        location.setPosition(request.getPosition());
        location.setType(request.getType());
        location.setCapacity(request.getCapacity() == null ? BigDecimal.ZERO : request.getCapacity());
        location.setUsedCapacity(request.getUsedCapacity() == null ? BigDecimal.ZERO : request.getUsedCapacity());
        location.setDescription(request.getDescription());
        location.setSort(request.getSort() == null ? 0 : request.getSort());
        location.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private Map<String, String> warehouseNameMap() {
        return warehouseMapper.selectList(null).stream().collect(Collectors.toMap(Warehouse::getCode, Warehouse::getName, (a, b) -> a));
    }
}