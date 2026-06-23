package com.qws.common.dto.material;

import com.qws.common.entity.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MaterialVO {
    private Long id;
    private String sku;
    private String name;
    private String spec;
    private String category;
    private String categoryName;
    private String brand;
    private String unit;
    private BigDecimal unitPrice;
    private Integer safeStock;
    private String supplier;
    private String barcode;
    private Integer shelfLifeDays;
    private BigDecimal weight;
    private BigDecimal volume;
    private String remark;
    private Integer status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static MaterialVO from(Material material, String categoryName) {
        MaterialVO vo = new MaterialVO();
        vo.id = material.getId();
        vo.sku = material.getSku();
        vo.name = material.getName();
        vo.spec = material.getSpec();
        vo.category = material.getCategory();
        vo.categoryName = categoryName;
        vo.brand = material.getBrand();
        vo.unit = material.getUnit();
        vo.unitPrice = material.getUnitPrice();
        vo.safeStock = material.getSafeStock();
        vo.supplier = material.getSupplier();
        vo.barcode = material.getBarcode();
        vo.shelfLifeDays = material.getShelfLifeDays();
        vo.weight = material.getWeight();
        vo.volume = material.getVolume();
        vo.remark = material.getRemark();
        vo.status = material.getStatus();
        vo.statusName = material.getStatus() != null && material.getStatus() == 1 ? "启用" : "禁用";
        vo.createTime = material.getCreateTime();
        vo.updateTime = material.getUpdateTime();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Integer getSafeStock() { return safeStock; }
    public void setSafeStock(Integer safeStock) { this.safeStock = safeStock; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Integer getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(Integer shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
