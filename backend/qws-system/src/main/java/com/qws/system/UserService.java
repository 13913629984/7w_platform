package com.qws.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.UserCreateRequest;
import com.qws.common.dto.UserImportRequest;
import com.qws.common.dto.UserListResult;
import com.qws.common.dto.UserRoleAssignRequest;
import com.qws.common.dto.UserUpdateRequest;
import com.qws.common.dto.UserVO;
import com.qws.common.dto.org.EmployeeVO;
import com.qws.common.entity.SysDepartment;
import com.qws.common.entity.SysEmployee;
import com.qws.common.entity.SysRole;
import com.qws.common.entity.SysUser;
import com.qws.common.entity.SysUserRole;
import com.qws.common.mapper.SysDepartmentMapper;
import com.qws.common.mapper.SysEmployeeMapper;
import com.qws.common.mapper.SysRoleMapper;
import com.qws.common.mapper.SysUserRoleMapper;
import com.qws.common.mapper.SysUserMapper;
import com.qws.common.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysEmployeeMapper sysEmployeeMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public UserService(SysUserMapper sysUserMapper, SysEmployeeMapper sysEmployeeMapper, SysDepartmentMapper sysDepartmentMapper, SysRoleMapper sysRoleMapper, SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysEmployeeMapper = sysEmployeeMapper;
        this.sysDepartmentMapper = sysDepartmentMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    public UserListResult list(String keyword, String status, int page, int pageSize) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getEmployeeId, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUser::getStatus, "\u542f\u7528".equals(status) ? 1 : 0);
        }
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> pageResult = sysUserMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<UserVO> list = pageResult.getRecords().stream().map(UserVO::from).toList();
        fillRoleNames(list);

        Long activeCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1)
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", pageResult.getTotal());
        stats.put("active", activeCount != null ? activeCount : 0);
        stats.put("pending", 0);
        stats.put("newToday", 0);

        UserListResult result = new UserListResult();
        result.setList(list);
        result.setTotal(pageResult.getTotal());
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setStats(stats);
        return result;
    }

    public Map<String, Object> create(UserCreateRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.hash(
                StringUtils.hasText(request.getPassword()) ? request.getPassword() : "123456"
        ));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setEmployeeId(request.getEmployeeId());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPosition());
        user.setRole(request.getRole());
        user.setStatus(1);
        sysUserMapper.insert(user);
        return Map.of("id", user.getId());
    }

    public void update(UserUpdateRequest request) {
        SysUser user = new SysUser();
        user.setId(request.getId());
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setEmployeeId(request.getEmployeeId());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPosition());
        user.setRole(request.getRole());
        user.setStatus("\u542f\u7528".equals(request.getStatus()) ? 1 : 0);
        sysUserMapper.updateById(user);
    }


    public List<Map<String, Object>> roles() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(role -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("code", role.getCode());
                    item.put("name", role.getName());
                    item.put("type", role.getType());
                    return item;
                })
                .toList();
    }

    @Transactional
    public void assignRoles(UserRoleAssignRequest request) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, request.getUserId()));
        if (request.getRoleCodes() == null || request.getRoleCodes().isEmpty()) {
            updateUserRoleText(request.getUserId(), "");
            return;
        }
        Set<String> roleCodes = request.getRoleCodes().stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        for (String roleCode : roleCodes) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(request.getUserId());
            relation.setRoleCode(roleCode);
            sysUserRoleMapper.insert(relation);
        }
        updateUserRoleText(request.getUserId(), roleNameText(roleCodes));
    }

    public List<String> userRoleCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleCode)
                .toList();
    }
    public void delete(Long id) {
        sysUserMapper.deleteById(id);
    }

    public Map<String, Object> importableEmployees(String keyword, int page, int pageSize) {
        Set<String> importedEmployeeNos = importedEmployeeNos();

        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        if (!importedEmployeeNos.isEmpty()) {
            wrapper.notIn(SysEmployee::getEmployeeNo, importedEmployeeNos);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(SysEmployee::getName, keyword)
                    .or().like(SysEmployee::getEmployeeNo, keyword)
                    .or().like(SysEmployee::getPhone, keyword));
        }
        wrapper.orderByAsc(SysEmployee::getEmployeeNo);

        Page<SysEmployee> pageResult = sysEmployeeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<Long, String> departmentMap = sysDepartmentMapper.selectList(null)
                .stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (left, right) -> left));
        List<EmployeeVO> employees = pageResult.getRecords().stream()
                .map(employee -> toEmployeeVO(employee, departmentMap.getOrDefault(employee.getDepartmentId(), "")))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", employees);
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @Transactional
    public Map<String, Object> importEmployees(UserImportRequest request) {
        if (request.getEmployeeIds() == null || request.getEmployeeIds().isEmpty()) {
            return Map.of("count", 0);
        }

        List<SysEmployee> employees = sysEmployeeMapper.selectBatchIds(request.getEmployeeIds());
        Map<Long, SysDepartment> departmentMap = sysDepartmentMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDepartment::getId, Function.identity(), (left, right) -> left));
        Set<String> importedEmployeeNos = importedEmployeeNos();

        int count = 0;
        for (SysEmployee employee : employees) {
            if (importedEmployeeNos.contains(employee.getEmployeeNo())) {
                continue;
            }
            SysUser user = new SysUser();
            user.setUsername(employee.getEmployeeNo());
            user.setPassword(PasswordUtil.hash("123456"));
            user.setNickname(employee.getName());
            user.setEmail(employee.getEmail());
            user.setPhone(employee.getPhone());
            user.setEmployeeId(employee.getEmployeeNo());
            SysDepartment department = departmentMap.get(employee.getDepartmentId());
            user.setDepartment(department != null ? department.getName() : "");
            user.setPosition(employee.getPosition());
            user.setRole("\u666e\u901a\u7528\u6237");
            user.setStatus(1);
            sysUserMapper.insert(user);
            importedEmployeeNos.add(employee.getEmployeeNo());
            count++;
        }

        return Map.of("count", count);
    }


    private void fillRoleNames(List<UserVO> users) {
        if (users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(UserVO::getId).toList();
        Map<Long, List<String>> roleCodesByUser = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId, Collectors.mapping(SysUserRole::getRoleCode, Collectors.toList())));
        Map<String, String> roleNameMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getCode, SysRole::getName, (left, right) -> left));
        for (UserVO user : users) {
            List<String> roleCodes = roleCodesByUser.get(user.getId());
            if (roleCodes == null || roleCodes.isEmpty()) {
                continue;
            }
            user.setRole(roleCodes.stream().map(code -> roleNameMap.getOrDefault(code, code)).collect(Collectors.joining("\u3001")));
        }
    }

    private void updateUserRoleText(Long userId, String roleText) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRole(roleText);
        sysUserMapper.updateById(user);
    }

    private String roleNameText(Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return "";
        }
        Map<String, String> roleNameMap = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getCode, roleCodes))
                .stream()
                .collect(Collectors.toMap(SysRole::getCode, SysRole::getName, (left, right) -> left));
        return roleCodes.stream().map(code -> roleNameMap.getOrDefault(code, code)).collect(Collectors.joining("\u3001"));
    }
    private Set<String> importedEmployeeNos() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getEmployeeId)
                        .isNotNull(SysUser::getEmployeeId))
                .stream()
                .map(SysUser::getEmployeeId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private EmployeeVO toEmployeeVO(SysEmployee employee, String departmentName) {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(employee.getId());
        vo.setEmployeeNo(employee.getEmployeeNo());
        vo.setName(employee.getName());
        vo.setPosition(employee.getPosition());
        vo.setDepartmentId(employee.getDepartmentId());
        vo.setDepartmentName(departmentName);
        vo.setEmail(employee.getEmail());
        vo.setPhone(employee.getPhone());
        vo.setStatus(employee.getStatus());
        vo.setEntryDate(employee.getEntryDate());
        vo.setLeaveDate(employee.getLeaveDate());
        return vo;
    }
}
