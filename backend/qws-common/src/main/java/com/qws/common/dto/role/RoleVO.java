package com.qws.common.dto.role;

import com.qws.common.entity.SysRole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RoleVO {

    private Long id;
    private String code;
    private String name;
    private String type;
    private String description;
    private String status;
    private int userCount;
    private int permissionCount;
    private List<String> permissionCodes = new ArrayList<>();
    private List<Long> userIds = new ArrayList<>();
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static RoleVO from(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setType(role.getType());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus() != null && role.getStatus() == 1 ? "启用" : "停用");
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public int getPermissionCount() {
        return permissionCount;
    }

    public void setPermissionCount(int permissionCount) {
        this.permissionCount = permissionCount;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
