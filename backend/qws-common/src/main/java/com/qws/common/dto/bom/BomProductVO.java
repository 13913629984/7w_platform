package com.qws.common.dto.bom;

import com.qws.common.entity.BomProduct;

import java.time.format.DateTimeFormatter;

public class BomProductVO {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String code;
    private String name;
    private String spec;
    private String category;
    private String categoryName;
    private Integer status;
    private String statusName;
    private String remark;
    private Integer nodeCount;
    private Integer materialCount;
    private String createTime;
    private String updateTime;

    public static BomProductVO from(BomProduct product) {
        BomProductVO vo = new BomProductVO();
        vo.setId(product.getId());
        vo.setCode(product.getCode());
        vo.setName(product.getName());
        vo.setSpec(product.getSpec());
        vo.setCategory(product.getCategory());
        vo.setStatus(product.getStatus());
        vo.setStatusName(product.getStatus() != null && product.getStatus() == 1 ? "启用" : "禁用");
        vo.setRemark(product.getRemark());
        if (product.getCreateTime() != null) {
            vo.setCreateTime(product.getCreateTime().format(FMT));
        }
        if (product.getUpdateTime() != null) {
            vo.setUpdateTime(product.getUpdateTime().format(FMT));
        }
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getNodeCount() { return nodeCount; }
    public void setNodeCount(Integer nodeCount) { this.nodeCount = nodeCount; }

    public Integer getMaterialCount() { return materialCount; }
    public void setMaterialCount(Integer materialCount) { this.materialCount = materialCount; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
