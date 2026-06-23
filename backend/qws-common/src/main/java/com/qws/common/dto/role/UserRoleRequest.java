package com.qws.common.dto.role;

import java.util.ArrayList;
import java.util.List;

public class UserRoleRequest {

    private String roleCode;
    private List<Long> userIds = new ArrayList<>();

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
