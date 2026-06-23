package com.qws.system;

import com.qws.common.ApiResult;
import com.qws.common.dto.org.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/org")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @GetMapping("/overview")
    public ApiResult<OrgOverviewResult> overview(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(orgService.overview(departmentId, keyword, page, pageSize));
    }

    @PostMapping("/department/create")
    public ApiResult<DepartmentVO> createDepartment(@RequestBody DepartmentRequest request) {
        try {
            return ApiResult.ok(orgService.createDepartment(request));
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("部门编号已存在");
        }
    }

    @PostMapping("/department/update")
    public ApiResult<Void> updateDepartment(@RequestBody DepartmentRequest request) {
        try {
            orgService.updateDepartment(request);
            return ApiResult.ok(null);
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("部门编号已存在");
        }
    }

    @PostMapping("/department/delete")
    public ApiResult<Void> deleteDepartment(@RequestBody DeleteRequest request) {
        try {
            orgService.deleteDepartment(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/employee/create")
    public ApiResult<EmployeeVO> createEmployee(@RequestBody EmployeeRequest request) {
        try {
            return ApiResult.ok(orgService.createEmployee(request));
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("员工工号已存在");
        }
    }

    @PostMapping("/employee/update")
    public ApiResult<Void> updateEmployee(@RequestBody EmployeeRequest request) {
        try {
            orgService.updateEmployee(request);
            return ApiResult.ok(null);
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("员工工号已存在");
        }
    }

    @PostMapping("/employee/delete")
    public ApiResult<Void> deleteEmployee(@RequestBody DeleteRequest request) {
        orgService.deleteEmployee(request.getId());
        return ApiResult.ok(null);
    }
}
