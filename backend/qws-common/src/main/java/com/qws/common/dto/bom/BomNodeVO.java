package com.qws.common.dto.bom;

import com.qws.common.entity.BomNode;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class BomNodeVO {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private Long productId;
    private Long parentId;
    private String nodeName;
    private Integer level;
    private Integer sort;
    private String remark;
    private List<BomNodeVO> children;
    private List<BomNodeMaterialVO> materials;
    private String createTime;
    private String updateTime;

    public static BomNodeVO from(BomNode node) {
        BomNodeVO vo = new BomNodeVO();
        vo.setId(node.getId());
        vo.setProductId(node.getProductId());
        vo.setParentId(node.getParentId());
        vo.setNodeName(node.getNodeName());
        vo.setLevel(node.getLevel());
        vo.setSort(node.getSort());
        vo.setRemark(node.getRemark());
        if (node.getCreateTime() != null) {
            vo.setCreateTime(node.getCreateTime().format(FMT));
        }
        if (node.getUpdateTime() != null) {
            vo.setUpdateTime(node.getUpdateTime().format(FMT));
        }
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<BomNodeVO> getChildren() { return children; }
    public void setChildren(List<BomNodeVO> children) { this.children = children; }

    public List<BomNodeMaterialVO> getMaterials() { return materials; }
    public void setMaterials(List<BomNodeMaterialVO> materials) { this.materials = materials; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
