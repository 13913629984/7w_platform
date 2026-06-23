package com.qws.system;

import com.qws.common.ApiResult;
import com.qws.common.dto.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public ApiResult<UserListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(userService.list(keyword, status, page, pageSize));
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody UserCreateRequest request) {
        try {
            return ApiResult.ok(userService.create(request));
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("\u7528\u6237\u540d\u5df2\u5b58\u5728");
        }
    }

    @PostMapping("/update")
    public ApiResult<Void> update(@RequestBody UserUpdateRequest request) {
        try {
            userService.update(request);
            return ApiResult.ok(null);
        } catch (DuplicateKeyException ex) {
            return ApiResult.fail("\u7528\u6237\u540d\u5df2\u5b58\u5728");
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody UserDeleteRequest request) {
        userService.delete(request.getId());
        return ApiResult.ok(null);
    }

    @GetMapping("/importable-employees")
    public ApiResult<Map<String, Object>> importableEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(userService.importableEmployees(keyword, page, pageSize));
    }

    @PostMapping("/import-employees")
    public ApiResult<Map<String, Object>> importEmployees(@RequestBody UserImportRequest request) {
        return ApiResult.ok(userService.importEmployees(request));
    }

    @GetMapping("/roles")
    public ApiResult<java.util.List<Map<String, Object>>> roles() {
        return ApiResult.ok(userService.roles());
    }

    @GetMapping("/role-codes")
    public ApiResult<java.util.List<String>> roleCodes(@RequestParam Long userId) {
        return ApiResult.ok(userService.userRoleCodes(userId));
    }

    @PostMapping("/assign-roles")
    public ApiResult<Void> assignRoles(@RequestBody UserRoleAssignRequest request) {
        userService.assignRoles(request);
        return ApiResult.ok(null);
    }}


