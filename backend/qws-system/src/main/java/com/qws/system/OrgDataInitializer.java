package com.qws.system;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class OrgDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public OrgDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_department (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    parent_id BIGINT DEFAULT NULL,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    leader VARCHAR(50),
                    sort INT DEFAULT 0,
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_parent_id (parent_id),
                    INDEX idx_code (code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_employee (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    employee_no VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(50) NOT NULL,
                    position VARCHAR(100),
                    department_id BIGINT NOT NULL,
                    email VARCHAR(100),
                    phone VARCHAR(20),
                    status VARCHAR(20) DEFAULT '在职',
                    entry_date DATE,
                    leave_date DATE,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_department_id (department_id),
                    INDEX idx_employee_no (employee_no),
                    INDEX idx_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        Integer deptCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_department", Integer.class);
        if (deptCount != null && deptCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (1, NULL, 'DEPT-001', '公司总部', '张明华', 1, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (2, 1, 'DEPT-002', '研发中心', '张明华', 1, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (3, 2, 'DEPT-003', '前端开发组', '李晓燕', 1, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (4, 2, 'DEPT-004', '后端开发组', '王建国', 2, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (5, 2, 'DEPT-005', '测试组', '陈思思', 3, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (6, 1, 'DEPT-006', '销售中心', '赵一鸣', 2, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (7, 6, 'DEPT-007', '商务拓展部', '刘佳', 1, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (8, 6, 'DEPT-008', '市场部', '周倩', 2, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (9, 1, 'DEPT-009', '管理中心', '钱宁', 3, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (10, 1, 'DEPT-010', '财务部', '孙敏', 4, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (11, 1, 'DEPT-011', '采购部', '吴迪', 5, 1)");
            jdbcTemplate.update("INSERT INTO sys_department (id, parent_id, code, name, leader, sort, status) VALUES (12, 1, 'DEPT-012', '人力资源部', '郑琳', 6, 1)");
        }

        Integer employeeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_employee", Integer.class);
        if (employeeCount != null && employeeCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_employee (employee_no, name, position, department_id, email, phone, status, entry_date) VALUES ('EMP-2026001', '张明华', '部门经理', 2, 'zhangmh@company.com', '138****1234', '在职', CURRENT_DATE())");
            jdbcTemplate.update("INSERT INTO sys_employee (employee_no, name, position, department_id, email, phone, status, entry_date) VALUES ('EMP-2026002', '李晓燕', '主管', 2, 'lixy@company.com', '139****5678', '在职', CURRENT_DATE())");
            jdbcTemplate.update("INSERT INTO sys_employee (employee_no, name, position, department_id, email, phone, status, entry_date) VALUES ('EMP-2026003', '王建国', '专员', 2, 'wangjg@company.com', '137****9012', '在职', CURRENT_DATE())");
            jdbcTemplate.update("INSERT INTO sys_employee (employee_no, name, position, department_id, email, phone, status, entry_date) VALUES ('EMP-2025056', '陈思思', '专员', 2, 'chenss@company.com', '136****3456', '试用期', DATE_SUB(CURRENT_DATE(), INTERVAL 20 DAY))");
            jdbcTemplate.update("INSERT INTO sys_employee (employee_no, name, position, department_id, email, phone, status, entry_date) VALUES ('EMP-2025012', '赵一鸣', '部门经理', 6, 'zhaoym@company.com', '135****1111', '在职', DATE_SUB(CURRENT_DATE(), INTERVAL 200 DAY))");
            jdbcTemplate.update("INSERT INTO sys_employee (employee_no, name, position, department_id, email, phone, status, entry_date) VALUES ('EMP-2025018', '刘佳', '主管', 7, 'liuj@company.com', '135****2222', '在职', DATE_SUB(CURRENT_DATE(), INTERVAL 180 DAY))");
            jdbcTemplate.update("INSERT INTO sys_employee (employee_no, name, position, department_id, email, phone, status, entry_date, leave_date) VALUES ('EMP-2024033', '周倩', '专员', 8, 'zhouq@company.com', '135****3333', '离职', DATE_SUB(CURRENT_DATE(), INTERVAL 500 DAY), CURRENT_DATE())");
        }
    }
}
