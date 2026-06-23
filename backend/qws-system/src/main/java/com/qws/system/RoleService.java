package com.qws.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.UserVO;
import com.qws.common.dto.role.PermissionVO;
import com.qws.common.dto.role.RoleListResult;
import com.qws.common.dto.role.RoleRequest;
import com.qws.common.dto.role.RoleVO;
import com.qws.common.dto.role.UserRoleRequest;
import com.qws.common.entity.SysPermission;
import com.qws.common.entity.SysRole;
import com.qws.common.entity.SysRolePermission;
import com.qws.common.entity.SysUser;
import com.qws.common.entity.SysUserRole;
import com.qws.common.mapper.SysPermissionMapper;
import com.qws.common.mapper.SysRoleMapper;
import com.qws.common.mapper.SysRolePermissionMapper;
import com.qws.common.mapper.SysUserMapper;
import com.qws.common.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;

    public RoleService(
            SysRoleMapper roleMapper,
            SysPermissionMapper permissionMapper,
            SysRolePermissionMapper rolePermissionMapper,
            SysUserRoleMapper userRoleMapper,
            SysUserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
    }

    public RoleListResult list(String keyword, String type, String status, int page, int pageSize) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(SysRole::getCode, keyword)
                    .or().like(SysRole::getName, keyword)
                    .or().like(SysRole::getDescription, keyword));
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(SysRole::getType, type);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysRole::getStatus, "启用".equals(status) ? 1 : 0);
        }
        wrapper.orderByAsc(SysRole::getType).orderByDesc(SysRole::getUpdateTime).orderByAsc(SysRole::getId);

        Page<SysRole> pageResult = roleMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<String> roleCodes = pageResult.getRecords().stream().map(SysRole::getCode).toList();
        Map<String, List<String>> permissionMap = permissionsByRole(roleCodes);
        Map<String, List<Long>> userMap = usersByRole(roleCodes);

        List<RoleVO> list = pageResult.getRecords().stream().map(role -> {
            RoleVO vo = RoleVO.from(role);
            List<String> permissionCodes = permissionMap.getOrDefault(role.getCode(), Collections.emptyList());
            List<Long> userIds = userMap.getOrDefault(role.getCode(), Collections.emptyList());
            vo.setPermissionCodes(permissionCodes);
            vo.setPermissionCount(permissionCodes.size());
            vo.setUserIds(userIds);
            vo.setUserCount(userIds.size());
            return vo;
        }).toList();

        RoleListResult result = new RoleListResult();
        result.setList(list);
        result.setTotal(pageResult.getTotal());
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setStats(buildStats());
        return result;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("stats", buildStats());
        result.put("permissions", permissionTree());
        return result;
    }

    public List<PermissionVO> permissionTree() {
        List<SysPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId));
        Map<String, PermissionVO> voMap = permissions.stream()
                .map(PermissionVO::from)
                .collect(Collectors.toMap(PermissionVO::getCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO permission : voMap.values()) {
            if (!StringUtils.hasText(permission.getParentCode()) || !voMap.containsKey(permission.getParentCode())) {
                roots.add(permission);
            } else {
                voMap.get(permission.getParentCode()).getChildren().add(permission);
            }
        }
        sortPermissions(roots);
        return roots;
    }

    public List<UserVO> users() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId))
                .stream()
                .map(UserVO::from)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(RoleRequest request) {
        SysRole role = new SysRole();
        copyRole(request, role);
        role.setCode(normalizeCode(request.getCode()));
        roleMapper.insert(role);
        return Map.of("id", role.getId());
    }

    @Transactional
    public void update(RoleRequest request) {
        SysRole role = roleMapper.selectById(request.getId());
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        role.setName(request.getName());
        role.setType(StringUtils.hasText(request.getType()) ? request.getType() : role.getType());
        role.setDescription(request.getDescription());
        role.setStatus("启用".equals(request.getStatus()) ? 1 : 0);
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            return;
        }
        if ("system".equals(role.getType())) {
            throw new IllegalStateException("系统角色不允许删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleCode, role.getCode()));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleCode, role.getCode()));
        roleMapper.deleteById(id);
    }

    @Transactional
    public void savePermissions(String roleCode, List<String> permissionCodes) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleCode, roleCode));
        if (permissionCodes == null) {
            return;
        }
        Set<String> uniqueCodes = permissionCodes.stream().filter(StringUtils::hasText).collect(Collectors.toSet());
        for (String permissionCode : uniqueCodes) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleCode(roleCode);
            relation.setPermissionCode(permissionCode);
            rolePermissionMapper.insert(relation);
        }
    }

    @Transactional
    public void saveUserRoles(UserRoleRequest request) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleCode, request.getRoleCode()));
        if (request.getUserIds() == null) {
            return;
        }
        for (Long userId : request.getUserIds()) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleCode(request.getRoleCode());
            userRoleMapper.insert(relation);
        }
    }

    private void copyRole(RoleRequest request, SysRole role) {
        role.setCode(normalizeCode(request.getCode()));
        role.setName(request.getName());
        role.setType(StringUtils.hasText(request.getType()) ? request.getType() : "custom");
        role.setDescription(request.getDescription());
        role.setStatus("停用".equals(request.getStatus()) ? 0 : 1);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private Map<String, Object> buildStats() {
        Long total = roleMapper.selectCount(null);
        Long active = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1));
        Long system = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getType, "system"));
        Long permissionCount = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getStatus, 1));
        Long assignedPermissionCount = rolePermissionMapper.selectCount(null);
        Long assignedUserCount = userRoleMapper.selectCount(null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total == null ? 0 : total);
        stats.put("active", active == null ? 0 : active);
        stats.put("custom", total == null || system == null ? 0 : total - system);
        stats.put("permissions", permissionCount == null ? 0 : permissionCount);
        stats.put("assignedPermissions", assignedPermissionCount == null ? 0 : assignedPermissionCount);
        stats.put("assignedUsers", assignedUserCount == null ? 0 : assignedUserCount);
        return stats;
    }

    private Map<String, List<String>> permissionsByRole(List<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleCode, roleCodes))
                .stream()
                .collect(Collectors.groupingBy(SysRolePermission::getRoleCode, Collectors.mapping(SysRolePermission::getPermissionCode, Collectors.toList())));
    }

    private Map<String, List<Long>> usersByRole(List<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleCode, roleCodes))
                .stream()
                .collect(Collectors.groupingBy(SysUserRole::getRoleCode, Collectors.mapping(SysUserRole::getUserId, Collectors.toList())));
    }

    private void sortPermissions(List<PermissionVO> permissions) {
        permissions.sort(Comparator.comparingInt(PermissionVO::getSort));
        permissions.forEach(permission -> sortPermissions(permission.getChildren()));
    }
}
