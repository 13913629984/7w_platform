package com.qws.common.dto;

import java.util.ArrayList;
import java.util.List;

public class UserRoleAssignRequest {

    private Long userId;
    private List<String> roleCodes = new ArrayList<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
