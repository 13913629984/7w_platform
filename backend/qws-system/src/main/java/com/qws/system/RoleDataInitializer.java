package com.qws.system;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public RoleDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        initPermissions();
        initRoles();
        initRolePermissions();
        initUserRoles();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_role (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    type VARCHAR(20) NOT NULL DEFAULT 'custom',
                    description VARCHAR(500),
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_code (code),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_permission (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    parent_code VARCHAR(80),
                    module VARCHAR(50),
                    description VARCHAR(500),
                    sort INT DEFAULT 0,
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_parent_code (parent_code),
                    INDEX idx_module (module)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_role_permission (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    role_code VARCHAR(50) NOT NULL,
                    permission_code VARCHAR(80) NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_role_permission (role_code, permission_code),
                    INDEX idx_role_code (role_code),
                    INDEX idx_permission_code (permission_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user_role (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    role_code VARCHAR(50) NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_role (user_id, role_code),
                    INDEX idx_user_id (user_id),
                    INDEX idx_role_code (role_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void initPermissions() {
        insertPermission("WMS", "📦 WMS仓储管理", null, "WMS", "仓储模块权限", 10);
        insertPermission("WMS_VIEW", "查看库存", "WMS", "WMS", "查看库存数据", 11);
        insertPermission("WMS_INBOUND", "入库操作", "WMS", "WMS", "执行入库单据", 12);
        insertPermission("WMS_OUTBOUND", "出库操作", "WMS", "WMS", "执行出库单据", 13);
        insertPermission("WMS_STOCK", "库存调整", "WMS", "WMS", "修改库存数量", 14);
        insertPermission("WMS_WARNING", "预警管理", "WMS", "WMS", "设置和处理预警", 15);
        insertPermission("WMS_ADMIN", "仓储管理", "WMS", "WMS", "完整管理权限", 16);

        insertPermission("CRM", "👥 CRM客户管理", null, "CRM", "客户模块权限", 20);
        insertPermission("CRM_VIEW", "查看客户", "CRM", "CRM", "查看客户信息", 21);
        insertPermission("CRM_EDIT", "编辑客户", "CRM", "CRM", "新建和编辑客户", 22);
        insertPermission("CRM_DELETE", "删除客户", "CRM", "CRM", "删除客户档案", 23);
        insertPermission("CRM_DEAL", "商机管理", "CRM", "CRM", "商机全流程", 24);
        insertPermission("CRM_CONTRACT", "合同管理", "CRM", "CRM", "合同签订与管理", 25);
        insertPermission("CRM_ADMIN", "CRM管理", "CRM", "CRM", "完整管理权限", 26);

        insertPermission("MAINT", "🔧 维保系统", null, "MAINT", "维保模块权限", 30);
        insertPermission("MAINT_VIEW", "查看设备", "MAINT", "MAINT", "查看设备台账", 31);
        insertPermission("MAINT_WORKORDER", "工单管理", "MAINT", "MAINT", "创建和处理工单", 32);
        insertPermission("MAINT_PLAN", "保养计划", "MAINT", "MAINT", "制定保养计划", 33);
        insertPermission("MAINT_SPAREPART", "备件管理", "MAINT", "MAINT", "备件库存管理", 34);
        insertPermission("MAINT_ADMIN", "维保管理", "MAINT", "MAINT", "完整管理权限", 35);

        insertPermission("ETS", "⚡ 电气测试", null, "ETS", "测试模块权限", 40);
        insertPermission("ETS_REGISTER", "来货登记", "ETS", "ETS", "登记来货信息", 41);
        insertPermission("ETS_TEST", "测试执行", "ETS", "ETS", "执行测试任务", 42);
        insertPermission("ETS_REPORT", "报告生成", "ETS", "ETS", "生成测试报告", 43);
        insertPermission("ETS_ADMIN", "测试管理", "ETS", "ETS", "完整管理权限", 44);

        insertPermission("FIN", "💰 财务管理", null, "FIN", "财务模块权限", 50);
        insertPermission("FIN_VIEW", "查看报表", "FIN", "FIN", "查看财务报表", 51);
        insertPermission("FIN_RECEIVABLE", "应收管理", "FIN", "FIN", "应收账款管理", 52);
        insertPermission("FIN_PAYABLE", "应付管理", "FIN", "FIN", "应付账款管理", 53);
        insertPermission("FIN_EXPENSE", "费用管理", "FIN", "FIN", "费用报销管理", 54);
        insertPermission("FIN_PAYMENT", "付款管理", "FIN", "FIN", "付款审批执行", 55);
        insertPermission("FIN_BUDGET", "预算管理", "FIN", "FIN", "预算编制与控制", 56);
        insertPermission("FIN_ADMIN", "财务管理", "FIN", "FIN", "完整管理权限", 57);

        insertPermission("SYS", "⚙️ 系统管理", null, "SYS", "系统模块权限", 60);
        insertPermission("SYS_USER", "用户管理", "SYS", "SYS", "用户CRUD", 61);
        insertPermission("SYS_ROLE", "角色管理", "SYS", "SYS", "角色权限配置", 62);
        insertPermission("SYS_ORG", "组织架构", "SYS", "SYS", "部门组织管理", 63);
        insertPermission("SYS_CONFIG", "系统设置", "SYS", "SYS", "系统参数配置", 64);
        insertPermission("SYS_AUDIT", "审计日志", "SYS", "SYS", "查看操作日志", 65);
        insertPermission("SYS_BACKUP", "数据备份", "SYS", "SYS", "备份恢复操作", 66);
    }

    private void initRoles() {
        insertRole("SUPER_ADMIN", "超级管理员", "system", "拥有平台全部功能和数据权限", 1);
        insertRole("SYS_ADMIN", "系统管理员", "system", "负责用户、组织、角色和系统参数维护", 1);
        insertRole("DEPT_MANAGER", "部门经理", "system", "部门业务审批、成员数据查看权限", 1);
        insertRole("WMS_ADMIN", "仓储管理员", "system", "仓储基础资料、库存和出入库完整管理", 1);
        insertRole("WMS_OPERATOR", "仓储操作员", "system", "执行入库、出库、盘点等日常仓储操作", 1);
        insertRole("CRM_ADMIN", "CRM管理员", "system", "客户、线索、商机及合同全流程管理", 1);
        insertRole("CRM_SALES", "销售人员", "system", "客户跟进、商机维护和合同查看", 1);
        insertRole("FIN_MANAGER", "财务经理", "system", "财务报表、应收应付、付款和预算审批", 1);
        insertRole("FIN_OPERATOR", "财务专员", "system", "费用、收付款和财务单据处理", 1);
        insertRole("MAINT_ENGINEER", "维保工程师", "system", "设备台账、工单、保养计划的执行权限", 1);
        insertRole("ETS_OPERATOR", "电测员", "system", "来货登记、测试执行与报告生成", 1);
        // 兜底角色：历史数据里角色名是中文、或新用户还没分配角色时使用，只给最基础的只读权限。
        // 绝不能缺这个角色——否则所有非管理员用户会拿到空权限、前端全线 401。
        insertRole("STAFF", "普通员工", "system", "基础只读权限，新用户默认角色", 1);
        insertRole("CUSTOM_ROLE_1", "数据分析师", "custom", "跨模块数据查询与报表分析", 1);
        insertRole("CUSTOM_ROLE_2", "审计员", "custom", "查看操作审计和关键业务日志", 1);
        insertRole("CUSTOM_ROLE_3", "访客", "custom", "只读访问基础页面", 0);
    }

    private void initRolePermissions() {
        assignAllPermissions("SUPER_ADMIN");
        assignPermissions("SYS_ADMIN", "SYS", "SYS_USER", "SYS_ROLE", "SYS_ORG", "SYS_CONFIG", "SYS_AUDIT");
        assignPermissions("DEPT_MANAGER", "CRM_VIEW", "CRM_DEAL", "WMS_VIEW", "FIN_VIEW");
        assignPermissions("WMS_ADMIN", "WMS", "WMS_VIEW", "WMS_INBOUND", "WMS_OUTBOUND", "WMS_STOCK", "WMS_WARNING", "WMS_ADMIN");
        assignPermissions("WMS_OPERATOR", "WMS_VIEW", "WMS_INBOUND", "WMS_OUTBOUND", "WMS_STOCK");
        assignPermissions("CRM_ADMIN", "CRM", "CRM_VIEW", "CRM_EDIT", "CRM_DELETE", "CRM_DEAL", "CRM_CONTRACT", "CRM_ADMIN");
        assignPermissions("CRM_SALES", "CRM_VIEW", "CRM_EDIT", "CRM_DEAL", "CRM_CONTRACT");
        assignPermissions("FIN_MANAGER", "FIN", "FIN_VIEW", "FIN_RECEIVABLE", "FIN_PAYABLE", "FIN_EXPENSE", "FIN_PAYMENT", "FIN_BUDGET", "FIN_ADMIN");
        assignPermissions("FIN_OPERATOR", "FIN_VIEW", "FIN_RECEIVABLE", "FIN_PAYABLE", "FIN_EXPENSE", "FIN_PAYMENT");
        assignPermissions("MAINT_ENGINEER", "MAINT", "MAINT_VIEW", "MAINT_WORKORDER", "MAINT_PLAN", "MAINT_SPAREPART");
        assignPermissions("ETS_OPERATOR", "ETS", "ETS_REGISTER", "ETS_TEST", "ETS_REPORT");
        // 兜底角色只给各子系统只读，不给任何写权限（写接口会被写权限判定拦住）
        assignPermissions("STAFF", "CRM_VIEW", "WMS_VIEW", "MAINT_VIEW", "FIN_VIEW", "ETS_REGISTER");
        assignPermissions("CUSTOM_ROLE_1", "WMS_VIEW", "CRM_VIEW", "FIN_VIEW", "SYS_AUDIT");
        assignPermissions("CUSTOM_ROLE_2", "SYS_AUDIT", "FIN_VIEW");
        assignPermissions("CUSTOM_ROLE_3", "CRM_VIEW", "WMS_VIEW");
    }

    private void initUserRoles() {
        // 幂等：不管表里原来有多少行，都把 "还没分配角色" 的启用用户补上兜底角色。
        // 同时也把历史中文角色名（sys_user.role='普通用户'）的用户接上 STAFF，
        // 否则他们登录后拿不到任何权限，前端会全线 401 被踢回登录页。
        jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_code) SELECT id, 'SUPER_ADMIN' FROM sys_user WHERE username = 'admin'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_code) SELECT id, 'CRM_SALES' FROM sys_user WHERE role = '销售人员'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_code) SELECT id, 'WMS_OPERATOR' FROM sys_user WHERE role = 'WMS操作员'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_code) SELECT id, 'FIN_OPERATOR' FROM sys_user WHERE role = '财务专员'");
        // 兜底：所有启用但还挂着中文/非法角色名的用户，统一接 STAFF（只读）
        jdbcTemplate.update("""
                INSERT IGNORE INTO sys_user_role (user_id, role_code)
                SELECT u.id, 'STAFF' FROM sys_user u
                WHERE u.id NOT IN (SELECT user_id FROM sys_user_role)
                  AND u.status = 1
                """);
    }

    private void insertPermission(String code, String name, String parentCode, String module, String description, int sort) {
        jdbcTemplate.update("""
                INSERT INTO sys_permission (code, name, parent_code, module, description, sort, status)
                VALUES (?, ?, ?, ?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE name = VALUES(name), parent_code = VALUES(parent_code), module = VALUES(module), description = VALUES(description), sort = VALUES(sort), status = 1
                """, code, name, parentCode, module, description, sort);
    }

    private void insertRole(String code, String name, String type, String description, int status) {
        jdbcTemplate.update("""
                INSERT INTO sys_role (code, name, type, description, status)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), description = VALUES(description)
                """, code, name, type, description, status);
    }

    private void assignAllPermissions(String roleCode) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO sys_role_permission (role_code, permission_code)
                SELECT ?, code FROM sys_permission WHERE status = 1
                """, roleCode);
    }

    private void assignPermissions(String roleCode, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update("INSERT IGNORE INTO sys_role_permission (role_code, permission_code) VALUES (?, ?)", roleCode, permissionCode);
        }
    }
}
