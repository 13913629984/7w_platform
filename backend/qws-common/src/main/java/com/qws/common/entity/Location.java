package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wms_location")
public class Location {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    @TableField("warehouse_code")
    private String warehouseCode;
    private String area;
    private String shelf;
    private String layer;
    private String position;
    private String type;
    private BigDecimal capacity;
    @TableField("used_capacity")
    private BigDecimal usedCapacity;
    private String description;
    private Integer sort;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
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
    public BigDecimal getCapacity() { return capacity; }
    public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }
    public BigDecimal getUsedCapacity() { return usedCapacity; }
    public void setUsedCapacity(BigDecimal usedCapacity) { this.usedCapacity = usedCapacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}