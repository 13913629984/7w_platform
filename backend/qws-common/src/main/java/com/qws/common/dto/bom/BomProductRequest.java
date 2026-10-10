package com.qws.common.dto.bom;

public class BomProductRequest {

    private Long id;
    private String code;
    private String name;
    private String spec;
    private String category;
    private Integer status;
    private String remark;

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

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
