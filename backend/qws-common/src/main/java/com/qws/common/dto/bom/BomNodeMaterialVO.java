package com.qws.common.dto.bom;

import com.qws.common.entity.BomNodeMaterial;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class BomNodeMaterialVO {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String MAIN = "MAIN";
    public static final String BACKUP = "BACKUP";

    private Long id;
    private Long nodeId;
    private Long materialId;
    private String materialType;
    private String materialTypeName;
    private String materialSku;
    private String materialName;
    private String materialSpec;
    private String materialUnit;
    private BigDecimal quantity;
    private String createTime;
    private String updateTime;

    public static BomNodeMaterialVO from(BomNodeMaterial entity) {
        BomNodeMaterialVO vo = new BomNodeMaterialVO();
        vo.setId(entity.getId());
        vo.setNodeId(entity.getNodeId());
        vo.setMaterialId(entity.getMaterialId());
        vo.setMaterialType(entity.getMaterialType());
        vo.setMaterialTypeName(MAIN.equals(entity.getMaterialType()) ? "主料" : "备料");
        vo.setQuantity(entity.getQuantity());
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(FMT));
        }
        if (entity.getUpdateTime() != null) {
            vo.setUpdateTime(entity.getUpdateTime().format(FMT));
        }
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    public String getMaterialTypeName() { return materialTypeName; }
    public void setMaterialTypeName(String materialTypeName) { this.materialTypeName = materialTypeName; }

    public String getMaterialSku() { return materialSku; }
    public void setMaterialSku(String materialSku) { this.materialSku = materialSku; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialSpec() { return materialSpec; }
    public void setMaterialSpec(String materialSpec) { this.materialSpec = materialSpec; }

    public String getMaterialUnit() { return materialUnit; }
    public void setMaterialUnit(String materialUnit) { this.materialUnit = materialUnit; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
