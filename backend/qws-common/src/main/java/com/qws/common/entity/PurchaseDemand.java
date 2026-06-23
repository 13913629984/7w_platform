package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("wms_purchase_demand")
public class PurchaseDemand {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String source;
    @TableField("material_id")
    private Long materialId;
    private String sku;
    @TableField("material_name")
    private String materialName;
    private String spec;
    @TableField("warehouse_code")
    private String warehouseCode;
    private BigDecimal qty;
    @TableField("estimated_price")
    private BigDecimal estimatedPrice;
    @TableField("demand_date")
    private LocalDate demandDate;
    private String status;
    @TableField("po_code")
    private String poCode;
    private String requester;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }
    public LocalDate getDemandDate() { return demandDate; }
    public void setDemandDate(LocalDate demandDate) { this.demandDate = demandDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPoCode() { return poCode; }
    public void setPoCode(String poCode) { this.poCode = poCode; }
    public String getRequester() { return requester; }
    public void setRequester(String requester) { this.requester = requester; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
