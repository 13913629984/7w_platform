package com.qws.common.dto.inventory;

import com.qws.common.entity.Inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InventoryVO {
    private Long id;
    private Long materialId;
    private String sku;
    private String materialName;
    private String spec;
    private String unit;
    private String category;
    private String brand;
    private String warehouseCode;
    private String warehouseName;
    private String locationCode;
    private String locationName;
    private String batchNo;
    private BigDecimal quantity;
    private BigDecimal availableQty;
    private BigDecimal lockedQty;
    private Integer safeStock;
    private String stockStatus;
    private String stockStatusName;
    private LocalDate productionDate;
    private LocalDate expireDate;
    private String remark;
    private Integer status;
    private String statusName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static InventoryVO from(Inventory inventory) {
        InventoryVO vo = new InventoryVO();
        vo.id = inventory.getId();
        vo.materialId = inventory.getMaterialId();
        vo.warehouseCode = inventory.getWarehouseCode();
        vo.locationCode = inventory.getLocationCode();
        vo.batchNo = inventory.getBatchNo();
        vo.quantity = inventory.getQuantity();
        vo.availableQty = inventory.getAvailableQty();
        vo.lockedQty = inventory.getLockedQty();
        vo.safeStock = inventory.getSafeStock();
        vo.stockStatus = calcStockStatus(inventory.getQuantity(), inventory.getSafeStock());
        vo.stockStatusName = stockStatusName(vo.stockStatus);
        vo.productionDate = inventory.getProductionDate();
        vo.expireDate = inventory.getExpireDate();
        vo.remark = inventory.getRemark();
        vo.status = inventory.getStatus();
        vo.statusName = inventory.getStatus() != null && inventory.getStatus() == 1 ? "启用" : "禁用";
        vo.createTime = inventory.getCreateTime();
        vo.updateTime = inventory.getUpdateTime();
        return vo;
    }

    private static String calcStockStatus(BigDecimal quantity, Integer safeStock) {
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal safe = BigDecimal.valueOf(safeStock == null ? 0 : safeStock);
        if (qty.signum() <= 0) return "empty";
        if (safe.signum() > 0 && qty.compareTo(safe) < 0) return "low";
        return "normal";
    }

    private static String stockStatusName(String status) {
        if ("empty".equals(status)) return "缺货";
        if ("low".equals(status)) return "低库存";
        return "正常";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAvailableQty() { return availableQty; }
    public void setAvailableQty(BigDecimal availableQty) { this.availableQty = availableQty; }
    public BigDecimal getLockedQty() { return lockedQty; }
    public void setLockedQty(BigDecimal lockedQty) { this.lockedQty = lockedQty; }
    public Integer getSafeStock() { return safeStock; }
    public void setSafeStock(Integer safeStock) { this.safeStock = safeStock; }
    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }
    public String getStockStatusName() { return stockStatusName; }
    public void setStockStatusName(String stockStatusName) { this.stockStatusName = stockStatusName; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }
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