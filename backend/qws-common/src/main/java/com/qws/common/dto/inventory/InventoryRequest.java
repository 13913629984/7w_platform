package com.qws.common.dto.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryRequest {
    private Long id;
    private Long materialId;
    private String warehouseCode;
    private String locationCode;
    private String batchNo;
    private BigDecimal quantity;
    private BigDecimal availableQty;
    private BigDecimal lockedQty;
    private Integer safeStock;
    private LocalDate productionDate;
    private LocalDate expireDate;
    private String remark;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
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
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}