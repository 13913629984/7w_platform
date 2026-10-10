package com.qws.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 库存预警规则。
 * <p>scope = global 表示全局规则（适用于所有物料）；scope = material 表示针对指定物料的规则。</p>
 * <p>阈值语义：库存数量 &lt;= highThreshold 判为高危，&lt;= mediumThreshold 判为中危，&lt;= lowThreshold 判为低危。</p>
 */
@TableName("wms_warning_rule")
public class WarningRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** global | material */
    private String scope;

    @TableField("material_id")
    private Long materialId;

    @TableField("warehouse_code")
    private String warehouseCode;

    @TableField("low_threshold")
    private Integer lowThreshold;

    @TableField("medium_threshold")
    private Integer mediumThreshold;

    @TableField("high_threshold")
    private Integer highThreshold;

    /** 1 启用，0 停用 */
    private Integer enabled;

    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public Integer getLowThreshold() { return lowThreshold; }
    public void setLowThreshold(Integer lowThreshold) { this.lowThreshold = lowThreshold; }
    public Integer getMediumThreshold() { return mediumThreshold; }
    public void setMediumThreshold(Integer mediumThreshold) { this.mediumThreshold = mediumThreshold; }
    public Integer getHighThreshold() { return highThreshold; }
    public void setHighThreshold(Integer highThreshold) { this.highThreshold = highThreshold; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
