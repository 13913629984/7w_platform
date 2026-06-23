package com.qws.system;

import com.qws.common.ApiResult;
import com.qws.common.dto.UserVO;
import com.qws.common.dto.role.PermissionVO;
import com.qws.common.dto.role.RoleDeleteRequest;
import com.qws.common.dto.role.RoleListResult;
import com.qws.common.dto.role.RolePermissionRequest;
import com.qws.common.dto.role.RoleRequest;
import com.qws.common.dto.role.UserRoleRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/list")
    public ApiResult<RoleListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(roleService.list(keyword, type, status, page, pageSize));
    }

    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        return ApiResult.ok(roleService.overview());
    }

    @GetMapping("/permissions")
    public ApiResult<List<PermissionVO>> permissions() {
        return ApiResult.ok(roleService.permissionTree());
    }

    @GetMapping("/users")
    public ApiResult<List<UserVO>> users() {
        return ApiResult.ok(roleService.users());
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody RoleRequest request) {
        try {
            return ApiResult.ok(roleService.create(request));
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("角色编码已存在");
        }
    }

    @PostMapping("/update")
    public ApiResult<Void> update(@RequestBody RoleRequest request) {
        try {
            roleService.update(request);
            return ApiResult.ok(null);
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("角色编码已存在");
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody RoleDeleteRequest request) {
        try {
            roleService.delete(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/permissions")
    public ApiResult<Void> savePermissions(@RequestBody RolePermissionRequest request) {
        roleService.savePermissions(request.getRoleCode(), request.getPermissionCodes());
        return ApiResult.ok(null);
    }

    @PostMapping("/users")
    public ApiResult<Void> saveUsers(@RequestBody UserRoleRequest request) {
        roleService.saveUserRoles(request);
        return ApiResult.ok(null);
    }
}
