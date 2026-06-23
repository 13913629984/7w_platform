package com.qws.common.dto;

import java.util.List;

public class UserImportRequest {

    private List<Long> employeeIds;

    public List<Long> getEmployeeIds() {
        return employeeIds;
    }

    public void setEmployeeIds(List<Long> employeeIds) {
        this.employeeIds = employeeIds;
    }
}
