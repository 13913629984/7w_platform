package com.qws.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.org.*;
import com.qws.common.entity.SysDepartment;
import com.qws.common.entity.SysEmployee;
import com.qws.common.mapper.SysDepartmentMapper;
import com.qws.common.mapper.SysEmployeeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrgService {

    private final SysDepartmentMapper departmentMapper;
    private final SysEmployeeMapper employeeMapper;

    public OrgService(SysDepartmentMapper departmentMapper, SysEmployeeMapper employeeMapper) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
    }

    public OrgOverviewResult overview(Long departmentId, String keyword, int page, int pageSize) {
        List<SysDepartment> departments = departmentMapper.selectList(new LambdaQueryWrapper<SysDepartment>()
                .orderByAsc(SysDepartment::getSort)
                .orderByAsc(SysDepartment::getId));
        List<SysEmployee> allEmployees = employeeMapper.selectList(new LambdaQueryWrapper<SysEmployee>()
                .orderByAsc(SysEmployee::getId));
        Map<Long, SysDepartment> departmentMap = departments.stream()
                .collect(Collectors.toMap(SysDepartment::getId, Function.identity()));
        Map<Long, Long> employeeCountMap = allEmployees.stream()
                .filter(employee -> employee.getDepartmentId() != null)
                .collect(Collectors.groupingBy(SysEmployee::getDepartmentId, Collectors.counting()));

        Page<SysEmployee> employeePage = queryEmployees(departmentId, keyword, page, pageSize);
        OrgOverviewResult result = new OrgOverviewResult();
        result.setTree(buildTree(departments, departmentMap, employeeCountMap));
        result.setEmployees(employeePage.getRecords().stream()
                .map(employee -> {
                    SysDepartment department = departmentMap.get(employee.getDepartmentId());
                    return toEmployeeVO(employee, department != null ? department.getName() : "-");
                })
                .toList());
        result.setEmployeeTotal(employeePage.getTotal());
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setStats(buildStats(departments, allEmployees));
        return result;
    }

    public DepartmentVO createDepartment(DepartmentRequest request) {
        SysDepartment department = new SysDepartment();
        copyDepartment(request, department);
        if (!StringUtils.hasText(department.getCode())) {
            department.setCode(nextDepartmentCode());
        }
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        if (department.getSort() == null) {
            department.setSort(0);
        }
        departmentMapper.insert(department);
        return toDepartmentVO(department, Collections.emptyMap(), Collections.emptyMap());
    }

    public void updateDepartment(DepartmentRequest request) {
        SysDepartment department = new SysDepartment();
        department.setId(request.getId());
        copyDepartment(request, department);
        departmentMapper.updateById(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Long childCount = departmentMapper.selectCount(new LambdaQueryWrapper<SysDepartment>()
                .eq(SysDepartment::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new IllegalStateException("请先删除下级部门");
        }
        Long employeeCount = employeeMapper.selectCount(new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getDepartmentId, id));
        if (employeeCount != null && employeeCount > 0) {
            throw new IllegalStateException("该部门下存在员工，不能删除");
        }
        departmentMapper.deleteById(id);
    }

    public EmployeeVO createEmployee(EmployeeRequest request) {
        SysEmployee employee = new SysEmployee();
        copyEmployee(request, employee);
        if (!StringUtils.hasText(employee.getEmployeeNo())) {
            employee.setEmployeeNo(nextEmployeeNo());
        }
        if (!StringUtils.hasText(employee.getStatus())) {
            employee.setStatus("在职");
        }
        if (employee.getEntryDate() == null) {
            employee.setEntryDate(LocalDate.now());
        }
        employeeMapper.insert(employee);
        SysDepartment department = departmentMapper.selectById(employee.getDepartmentId());
        return toEmployeeVO(employee, department != null ? department.getName() : "-");
    }

    public void updateEmployee(EmployeeRequest request) {
        SysEmployee employee = new SysEmployee();
        employee.setId(request.getId());
        copyEmployee(request, employee);
        employeeMapper.updateById(employee);
    }

    public void deleteEmployee(Long id) {
        employeeMapper.deleteById(id);
    }

    private Page<SysEmployee> queryEmployees(Long departmentId, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null) {
            wrapper.eq(SysEmployee::getDepartmentId, departmentId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysEmployee::getName, keyword)
                    .or().like(SysEmployee::getEmployeeNo, keyword)
                    .or().like(SysEmployee::getPhone, keyword)
                    .or().like(SysEmployee::getEmail, keyword));
        }
        wrapper.orderByAsc(SysEmployee::getId);
        return employeeMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    private List<DepartmentVO> buildTree(List<SysDepartment> departments, Map<Long, SysDepartment> departmentMap, Map<Long, Long> employeeCountMap) {
        Map<Long, DepartmentVO> voMap = departments.stream()
                .map(department -> toDepartmentVO(department, departmentMap, employeeCountMap))
                .collect(Collectors.toMap(DepartmentVO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<DepartmentVO> roots = new ArrayList<>();
        for (DepartmentVO department : voMap.values()) {
            if (department.getParentId() == null || !voMap.containsKey(department.getParentId())) {
                roots.add(department);
            } else {
                voMap.get(department.getParentId()).getChildren().add(department);
            }
        }
        return roots;
    }

    private Map<String, Object> buildStats(List<SysDepartment> departments, List<SysEmployee> employees) {
        YearMonth currentMonth = YearMonth.now();
        long activeEmployees = employees.stream().filter(employee -> !"离职".equals(employee.getStatus())).count();
        long newThisMonth = employees.stream()
                .filter(employee -> employee.getEntryDate() != null && YearMonth.from(employee.getEntryDate()).equals(currentMonth))
                .count();
        long leaveThisMonth = employees.stream()
                .filter(employee -> employee.getLeaveDate() != null && YearMonth.from(employee.getLeaveDate()).equals(currentMonth))
                .count();
        long managerCount = employees.stream()
                .filter(employee -> employee.getPosition() != null && (employee.getPosition().contains("经理") || employee.getPosition().contains("主管")))
                .count();
        long positionCount = employees.stream()
                .map(SysEmployee::getPosition)
                .filter(StringUtils::hasText)
                .distinct()
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("departmentCount", departments.size());
        stats.put("companyCount", 1);
        stats.put("employeeCount", employees.size());
        stats.put("activeEmployeeCount", activeEmployees);
        stats.put("newThisMonth", newThisMonth);
        stats.put("leaveThisMonth", leaveThisMonth);
        stats.put("positionCount", positionCount);
        stats.put("managerPositionCount", managerCount);
        return stats;
    }

    private DepartmentVO toDepartmentVO(SysDepartment department, Map<Long, SysDepartment> departmentMap, Map<Long, Long> employeeCountMap) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setParentId(department.getParentId());
        vo.setCode(department.getCode());
        vo.setName(department.getName());
        vo.setLeader(department.getLeader());
        vo.setSort(department.getSort());
        vo.setStatus(department.getStatus());
        vo.setEmployeeCount(employeeCountMap.getOrDefault(department.getId(), 0L).intValue());
        SysDepartment parent = departmentMap.get(department.getParentId());
        vo.setParentName(parent != null ? parent.getName() : "-");
        return vo;
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

    private void copyDepartment(DepartmentRequest request, SysDepartment department) {
        department.setParentId(request.getParentId());
        department.setCode(request.getCode());
        department.setName(request.getName());
        department.setLeader(request.getLeader());
        department.setSort(request.getSort());
        department.setStatus(request.getStatus());
    }

    private void copyEmployee(EmployeeRequest request, SysEmployee employee) {
        employee.setEmployeeNo(request.getEmployeeNo());
        employee.setName(request.getName());
        employee.setPosition(request.getPosition());
        employee.setDepartmentId(request.getDepartmentId());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setStatus(request.getStatus());
        employee.setEntryDate(request.getEntryDate());
        employee.setLeaveDate(request.getLeaveDate());
    }

    private String nextDepartmentCode() {
        Long count = departmentMapper.selectCount(null);
        return "DEPT-" + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String nextEmployeeNo() {
        Long count = employeeMapper.selectCount(null);
        return "EMP-" + LocalDate.now().getYear() + String.format("%03d", (count == null ? 0 : count) + 1);
    }
}
