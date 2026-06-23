package com.qws.common.dto.materialtype;

import com.qws.common.entity.MaterialType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MaterialTypeVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String parentCode;
    private String parentName;
    private Integer level;
    private Integer sort;
    private Integer status;
    private String statusName;
    private Integer materialCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MaterialTypeVO> children = new ArrayList<>();

    public static MaterialTypeVO from(MaterialType type) {
        MaterialTypeVO vo = new MaterialTypeVO();
        vo.id = type.getId();
        vo.code = type.getCode();
        vo.name = type.getName();
        vo.description = type.getDescription();
        vo.parentCode = type.getParentCode();
        vo.level = type.getLevel();
        vo.sort = type.getSort();
        vo.status = type.getStatus();
        vo.statusName = type.getStatus() != null && type.getStatus() == 1 ? "启用" : "禁用";
        vo.createTime = type.getCreateTime();
        vo.updateTime = type.getUpdateTime();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Integer getMaterialCount() { return materialCount; }
    public void setMaterialCount(Integer materialCount) { this.materialCount = materialCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<MaterialTypeVO> getChildren() { return children; }
    public void setChildren(List<MaterialTypeVO> children) { this.children = children; }
}