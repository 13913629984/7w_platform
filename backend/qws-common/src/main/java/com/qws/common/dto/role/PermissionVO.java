package com.qws.common.dto.role;

import com.qws.common.entity.SysPermission;

import java.util.ArrayList;
import java.util.List;

public class PermissionVO {

    private Long id;
    private String code;
    private String name;
    private String parentCode;
    private String module;
    private String description;
    private int sort;
    private List<PermissionVO> children = new ArrayList<>();

    public static PermissionVO from(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setParentCode(permission.getParentCode());
        vo.setModule(permission.getModule());
        vo.setDescription(permission.getDescription());
        vo.setSort(permission.getSort() == null ? 0 : permission.getSort());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public List<PermissionVO> getChildren() {
        return children;
    }

    public void setChildren(List<PermissionVO> children) {
        this.children = children;
    }
}
