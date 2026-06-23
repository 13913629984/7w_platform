package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.inventory.InventoryRequest;
import com.qws.common.dto.inventory.InventoryStatusRequest;
import com.qws.common.dto.inventory.InventoryVO;
import com.qws.common.entity.Inventory;
import com.qws.common.entity.Location;
import com.qws.common.entity.Material;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.LocationMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;

    public InventoryService(InventoryMapper inventoryMapper, MaterialMapper materialMapper, WarehouseMapper warehouseMapper, LocationMapper locationMapper) {
        this.inventoryMapper = inventoryMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.locationMapper = locationMapper;
    }

    public List<InventoryVO> list(String keyword, String warehouseCode, String locationCode, String stockStatus, Integer status) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(warehouseCode)) wrapper.eq(Inventory::getWarehouseCode, warehouseCode);
        if (StringUtils.hasText(locationCode)) wrapper.eq(Inventory::getLocationCode, locationCode);
        if (status != null) wrapper.eq(Inventory::getStatus, status);
        wrapper.orderByAsc(Inventory::getWarehouseCode).orderByAsc(Inventory::getLocationCode).orderByAsc(Inventory::getId);
        List<InventoryVO> list = fillRelated(inventoryMapper.selectList(wrapper));
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            list = list.stream().filter(vo -> contains(vo.getSku(), kw)
                    || contains(vo.getMaterialName(), kw)
                    || contains(vo.getBatchNo(), kw)
                    || contains(vo.getWarehouseName(), kw)
                    || contains(vo.getLocationName(), kw)).toList();
        }
        if (StringUtils.hasText(stockStatus)) {
            list = list.stream().filter(vo -> stockStatus.equals(vo.getStockStatus())).toList();
        }
        return list;
    }

    public InventoryVO detail(Long id) {
        Inventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) throw new IllegalArgumentException("库存不存在");
        return fillRelated(List.of(inventory)).get(0);
    }

    @Transactional
    public InventoryVO create(InventoryRequest request) {
        validate(request, false);
        Inventory inventory = new Inventory();
        applyRequest(inventory, request);
        inventory.setCreateTime(LocalDateTime.now());
        inventory.setUpdateTime(LocalDateTime.now());
        inventoryMapper.insert(inventory);
        return detail(inventory.getId());
    }

    @Transactional
    public InventoryVO update(InventoryRequest request) {
        if (request.getId() == null) throw new IllegalArgumentException("缺少库存ID");
        Inventory inventory = inventoryMapper.selectById(request.getId());
        if (inventory == null) throw new IllegalArgumentException("库存不存在");
        validate(request, true);
        applyRequest(inventory, request);
        inventory.setUpdateTime(LocalDateTime.now());
        inventoryMapper.updateById(inventory);
        return detail(inventory.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("缺少库存ID");
        Inventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) throw new IllegalArgumentException("库存不存在");
        if (inventory.getLockedQty() != null && inventory.getLockedQty().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("库存存在锁定数量，无法删除");
        }
        inventoryMapper.deleteById(id);
    }

    @Transactional
    public InventoryVO changeStatus(InventoryStatusRequest request) {
        if (request.getId() == null || request.getStatus() == null) throw new IllegalArgumentException("参数不完整");
        Inventory inventory = inventoryMapper.selectById(request.getId());
        if (inventory == null) throw new IllegalArgumentException("库存不存在");
        inventory.setStatus(request.getStatus());
        inventory.setUpdateTime(LocalDateTime.now());
        inventoryMapper.updateById(inventory);
        return detail(inventory.getId());
    }

    public Map<String, Object> stats() {
        List<InventoryVO> list = fillRelated(inventoryMapper.selectList(null));
        BigDecimal totalQty = list.stream().map(vo -> vo.getQuantity() == null ? BigDecimal.ZERO : vo.getQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableQty = list.stream().map(vo -> vo.getAvailableQty() == null ? BigDecimal.ZERO : vo.getAvailableQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lockedQty = list.stream().map(vo -> vo.getLockedQty() == null ? BigDecimal.ZERO : vo.getLockedQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", list.size());
        stats.put("materialCount", list.stream().map(InventoryVO::getMaterialId).distinct().count());
        stats.put("totalQty", totalQty);
        stats.put("availableQty", availableQty);
        stats.put("lockedQty", lockedQty);
        stats.put("lowCount", list.stream().filter(vo -> "low".equals(vo.getStockStatus())).count());
        stats.put("emptyCount", list.stream().filter(vo -> "empty".equals(vo.getStockStatus())).count());
        stats.put("activeCount", list.stream().filter(vo -> vo.getStatus() != null && vo.getStatus() == 1).count());
        return stats;
    }

    private void validate(InventoryRequest request, boolean isUpdate) {
        if (request.getMaterialId() == null) throw new IllegalArgumentException("请选择物料");
        Material material = materialMapper.selectById(request.getMaterialId());
        if (material == null) throw new IllegalArgumentException("物料不存在");
        if (!StringUtils.hasText(request.getWarehouseCode())) throw new IllegalArgumentException("请选择仓库");
        Warehouse warehouse = warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getCode, request.getWarehouseCode()));
        if (warehouse == null) throw new IllegalArgumentException("仓库不存在");
        if (!StringUtils.hasText(request.getLocationCode())) throw new IllegalArgumentException("请选择库位");
        Location location = locationMapper.selectOne(new LambdaQueryWrapper<Location>().eq(Location::getCode, request.getLocationCode()));
        if (location == null) throw new IllegalArgumentException("库位不存在");
        if (!request.getWarehouseCode().equals(location.getWarehouseCode())) throw new IllegalArgumentException("库位不属于所选仓库");
        if (request.getQuantity() == null || request.getQuantity().signum() < 0) throw new IllegalArgumentException("库存数量不能小于0");
        if (request.getAvailableQty() != null && request.getAvailableQty().signum() < 0) throw new IllegalArgumentException("可用数量不能小于0");
        if (request.getLockedQty() != null && request.getLockedQty().signum() < 0) throw new IllegalArgumentException("锁定数量不能小于0");
        BigDecimal available = request.getAvailableQty() == null ? request.getQuantity() : request.getAvailableQty();
        BigDecimal locked = request.getLockedQty() == null ? BigDecimal.ZERO : request.getLockedQty();
        if (available.add(locked).compareTo(request.getQuantity()) > 0) throw new IllegalArgumentException("可用数量+锁定数量不能大于库存数量");
    }

    private void applyRequest(Inventory inventory, InventoryRequest request) {
        Material material = materialMapper.selectById(request.getMaterialId());
        inventory.setMaterialId(request.getMaterialId());
        inventory.setWarehouseCode(request.getWarehouseCode());
        inventory.setLocationCode(request.getLocationCode());
        inventory.setBatchNo(request.getBatchNo());
        inventory.setQuantity(request.getQuantity() == null ? BigDecimal.ZERO : request.getQuantity());
        inventory.setLockedQty(request.getLockedQty() == null ? BigDecimal.ZERO : request.getLockedQty());
        inventory.setAvailableQty(request.getAvailableQty() == null ? inventory.getQuantity().subtract(inventory.getLockedQty()) : request.getAvailableQty());
        inventory.setSafeStock(request.getSafeStock() == null ? (material == null ? 0 : material.getSafeStock()) : request.getSafeStock());
        inventory.setProductionDate(request.getProductionDate());
        inventory.setExpireDate(request.getExpireDate());
        inventory.setRemark(request.getRemark());
        inventory.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private List<InventoryVO> fillRelated(List<Inventory> inventories) {
        Map<Long, Material> materialMap = materialMapper.selectList(null).stream().collect(Collectors.toMap(Material::getId, Function.identity(), (a, b) -> a));
        Map<String, Warehouse> warehouseMap = warehouseMapper.selectList(null).stream().collect(Collectors.toMap(Warehouse::getCode, Function.identity(), (a, b) -> a));
        Map<String, Location> locationMap = locationMapper.selectList(null).stream().collect(Collectors.toMap(Location::getCode, Function.identity(), (a, b) -> a));
        return inventories.stream().map(inventory -> {
            InventoryVO vo = InventoryVO.from(inventory);
            Material material = materialMap.get(inventory.getMaterialId());
            if (material != null) {
                vo.setSku(material.getSku());
                vo.setMaterialName(material.getName());
                vo.setSpec(material.getSpec());
                vo.setUnit(material.getUnit());
                vo.setCategory(material.getCategory());
                vo.setBrand(material.getBrand());
            }
            Warehouse warehouse = warehouseMap.get(inventory.getWarehouseCode());
            if (warehouse != null) vo.setWarehouseName(warehouse.getName());
            Location location = locationMap.get(inventory.getLocationCode());
            if (location != null) vo.setLocationName(location.getName());
            return vo;
        }).toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}