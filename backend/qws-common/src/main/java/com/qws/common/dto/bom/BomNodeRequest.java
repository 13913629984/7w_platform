package com.qws.common.dto.bom;

public class BomNodeRequest {

    private Long id;
    private Long productId;
    private Long parentId;
    private String nodeName;
    private Integer sort;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
