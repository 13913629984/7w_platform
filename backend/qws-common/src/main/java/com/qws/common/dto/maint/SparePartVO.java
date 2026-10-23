package com.qws.common.dto.maint;

import com.qws.common.entity.MaintSparePart;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 维保-备件 输出对象 */
public class SparePartVO {
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private String category;
    private String warehouseCode;
    private BigDecimal stockQty;
    private BigDecimal safetyQty;
    private BigDecimal price;
    private String supplier;
    private Integer status;
    private String remark;
    /** 是否低于安全库存 */
    private Boolean lowStock;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static SparePartVO from(MaintSparePart entity) {
        SparePartVO vo = new SparePartVO();
        vo.id = entity.getId();
        vo.code = entity.getCode();
        vo.name = entity.getName();
        vo.spec = entity.getSpec();
        vo.unit = entity.getUnit();
        vo.category = entity.getCategory();
        vo.warehouseCode = entity.getWarehouseCode();
        vo.stockQty = entity.getStockQty();
        vo.safetyQty = entity.getSafetyQty();
        vo.price = entity.getPrice();
        vo.supplier = entity.getSupplier();
        vo.status = entity.getStatus();
        vo.remark = entity.getRemark();
        vo.lowStock = entity.getStockQty() != null && entity.getSafetyQty() != null
                && entity.getStockQty().compareTo(entity.getSafetyQty()) < 0;
        vo.createTime = format(entity.getCreateTime());
        vo.updateTime = format(entity.getUpdateTime());
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(DATETIME);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public BigDecimal getStockQty() { return stockQty; }
    public void setStockQty(BigDecimal stockQty) { this.stockQty = stockQty; }
    public BigDecimal getSafetyQty() { return safetyQty; }
    public void setSafetyQty(BigDecimal safetyQty) { this.safetyQty = safetyQty; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Boolean getLowStock() { return lowStock; }
    public void setLowStock(Boolean lowStock) { this.lowStock = lowStock; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
