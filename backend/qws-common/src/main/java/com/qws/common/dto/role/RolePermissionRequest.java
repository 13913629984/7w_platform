package com.qws.common.dto.role;

import java.util.ArrayList;
import java.util.List;

public class RolePermissionRequest {

    private String roleCode;
    private List<String> permissionCodes = new ArrayList<>();

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
