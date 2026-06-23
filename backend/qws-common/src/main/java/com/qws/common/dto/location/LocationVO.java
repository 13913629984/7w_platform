package com.qws.common.dto.location;

import com.qws.common.entity.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class LocationVO {
    private Long id;
    private String code;
    private String name;
    private String warehouseCode;
    private String warehouseName;
    private String area;
    private String shelf;
    private String layer;
    private String position;
    private String type;
    private String typeName;
    private BigDecimal capacity;
    private BigDecimal usedCapacity;
    private BigDecimal usageRate;
    private String description;
    private Integer sort;
    private Integer status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static LocationVO from(Location location) {
        LocationVO vo = new LocationVO();
        vo.id = location.getId();
        vo.code = location.getCode();
        vo.name = location.getName();
        vo.warehouseCode = location.getWarehouseCode();
        vo.area = location.getArea();
        vo.shelf = location.getShelf();
        vo.layer = location.getLayer();
        vo.position = location.getPosition();
        vo.type = location.getType();
        vo.typeName = typeName(location.getType());
        vo.capacity = location.getCapacity();
        vo.usedCapacity = location.getUsedCapacity();
        vo.usageRate = calcUsageRate(location.getCapacity(), location.getUsedCapacity());
        vo.description = location.getDescription();
        vo.sort = location.getSort();
        vo.status = location.getStatus();
        vo.statusName = location.getStatus() != null && location.getStatus() == 1 ? "启用" : "禁用";
        vo.createTime = location.getCreateTime();
        vo.updateTime = location.getUpdateTime();
        return vo;
    }

    private static BigDecimal calcUsageRate(BigDecimal capacity, BigDecimal usedCapacity) {
        if (capacity == null || usedCapacity == null || capacity.signum() <= 0) return BigDecimal.ZERO;
        return usedCapacity.multiply(BigDecimal.valueOf(100)).divide(capacity, 2, RoundingMode.HALF_UP);
    }

    private static String typeName(String type) {
        if ("pallet".equals(type)) return "托盘位";
        if ("shelf".equals(type)) return "货架位";
        if ("floor".equals(type)) return "地堆位";
        if ("temporary".equals(type)) return "暂存位";
        return "普通库位";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getShelf() { return shelf; }
    public void setShelf(String shelf) { this.shelf = shelf; }
    public String getLayer() { return layer; }
    public void setLayer(String layer) { this.layer = layer; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public BigDecimal getCapacity() { return capacity; }
    public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }
    public BigDecimal getUsedCapacity() { return usedCapacity; }
    public void setUsedCapacity(BigDecimal usedCapacity) { this.usedCapacity = usedCapacity; }
    public BigDecimal getUsageRate() { return usageRate; }
    public void setUsageRate(BigDecimal usageRate) { this.usageRate = usageRate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}