package com.qws.common.dto.warehouse;

import com.qws.common.entity.Warehouse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class WarehouseVO {
    private Long id;
    private String code;
    private String name;
    private String type;
    private String typeName;
    private String address;
    private String manager;
    private String phone;
    private BigDecimal area;
    private BigDecimal capacity;
    private BigDecimal usedCapacity;
    private BigDecimal usageRate;
    private String description;
    private Integer sort;
    private Integer status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static WarehouseVO from(Warehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        vo.id = warehouse.getId();
        vo.code = warehouse.getCode();
        vo.name = warehouse.getName();
        vo.type = warehouse.getType();
        vo.typeName = typeName(warehouse.getType());
        vo.address = warehouse.getAddress();
        vo.manager = warehouse.getManager();
        vo.phone = warehouse.getPhone();
        vo.area = warehouse.getArea();
        vo.capacity = warehouse.getCapacity();
        vo.usedCapacity = warehouse.getUsedCapacity();
        vo.usageRate = calcUsageRate(warehouse.getCapacity(), warehouse.getUsedCapacity());
        vo.description = warehouse.getDescription();
        vo.sort = warehouse.getSort();
        vo.status = warehouse.getStatus();
        vo.statusName = warehouse.getStatus() != null && warehouse.getStatus() == 1 ? "启用" : "禁用";
        vo.createTime = warehouse.getCreateTime();
        vo.updateTime = warehouse.getUpdateTime();
        return vo;
    }

    private static BigDecimal calcUsageRate(BigDecimal capacity, BigDecimal usedCapacity) {
        if (capacity == null || usedCapacity == null || capacity.signum() <= 0) return BigDecimal.ZERO;
        return usedCapacity.multiply(BigDecimal.valueOf(100)).divide(capacity, 2, RoundingMode.HALF_UP);
    }

    private static String typeName(String type) {
        if ("raw".equals(type)) return "原料仓";
        if ("finished".equals(type)) return "成品仓";
        if ("spare".equals(type)) return "备品备件仓";
        if ("temporary".equals(type)) return "暂存仓";
        return "普通仓";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
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