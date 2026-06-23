package com.qws.common.dto.org;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrgOverviewResult {
    private List<DepartmentVO> tree;
    private List<EmployeeVO> employees;
    private Long employeeTotal;
    private Integer page;
    private Integer pageSize;
    private Map<String, Object> stats = new HashMap<>();

    public List<DepartmentVO> getTree() {
        return tree;
    }

    public void setTree(List<DepartmentVO> tree) {
        this.tree = tree;
    }

    public List<EmployeeVO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<EmployeeVO> employees) {
        this.employees = employees;
    }

    public Long getEmployeeTotal() {
        return employeeTotal;
    }

    public void setEmployeeTotal(Long employeeTotal) {
        this.employeeTotal = employeeTotal;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }
}
