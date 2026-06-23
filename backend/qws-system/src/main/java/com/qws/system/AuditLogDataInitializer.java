package com.qws.system;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditLogDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        initLogs();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_audit_log (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    operate_time DATETIME NOT NULL,
                    username VARCHAR(50) NOT NULL,
                    module_code VARCHAR(30) NOT NULL,
                    module_name VARCHAR(50) NOT NULL,
                    action_type VARCHAR(30) NOT NULL,
                    action_name VARCHAR(50) NOT NULL,
                    content VARCHAR(500) NOT NULL,
                    target_name VARCHAR(100),
                    ip_address VARCHAR(50),
                    status VARCHAR(20) NOT NULL DEFAULT 'success',
                    cost_ms INT DEFAULT 0,
                    request_id VARCHAR(80),
                    browser VARCHAR(120),
                    detail TEXT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_operate_time (operate_time),
                    INDEX idx_username (username),
                    INDEX idx_module_code (module_code),
                    INDEX idx_action_type (action_type),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void initLogs() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_audit_log", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        insertLog("2026-05-28 14:32:15", "admin", "SYS", "系统", "LOGIN", "登录", "用户登录系统", "-", "192.168.1.100", "success", 230, "req_audit_001", "Chrome 125.0", "{\"action\":\"login\",\"result\":\"success\"}");
        insertLog("2026-05-28 14:35:22", "admin", "SYS", "系统", "CREATE", "新增", "新增用户：张三 (EMP-2026028)", "用户管理", "192.168.1.100", "success", 560, "req_audit_002", "Chrome 125.0", "{\"action\":\"create_user\",\"data\":{\"name\":\"张三\",\"empNo\":\"EMP-2026028\"}}");
        insertLog("2026-05-28 14:40:08", "admin", "SYS", "系统", "UPDATE", "修改", "修改系统参数：会话超时 60→120", "系统设置", "192.168.1.100", "success", 180, "req_audit_003", "Chrome 125.0", "{\"action\":\"update_setting\",\"field\":\"sessionTimeout\",\"from\":60,\"to\":120}");
        insertLog("2026-05-28 14:42:33", "zhangmh", "WMS", "WMS", "CREATE", "新增", "创建入库单：RK-2026-0528-001，物料：电子元件一批", "入库单据", "192.168.1.105", "success", 820, "req_audit_004", "Edge 125.0", "{\"action\":\"create_inbound\",\"code\":\"RK-2026-0528-001\"}");
        insertLog("2026-05-28 14:45:12", "wangjg", "CRM", "CRM", "CREATE", "新增", "新建客户：深圳XX科技有限公司", "客户档案", "192.168.1.108", "success", 450, "req_audit_005", "Chrome 125.0", "{\"action\":\"create_customer\",\"customer\":\"深圳XX科技有限公司\"}");
        insertLog("2026-05-28 14:50:05", "lixy", "WMS", "WMS", "CREATE", "新增", "创建出库单：CK-2026-0528-002，客户：广州YY电子", "出库单据", "192.168.1.109", "success", 670, "req_audit_006", "Chrome 125.0", "{\"action\":\"create_outbound\",\"code\":\"CK-2026-0528-002\"}");
    }

    private void insertLog(String operateTime, String username, String moduleCode, String moduleName, String actionType, String actionName, String content, String targetName, String ipAddress, String status, int costMs, String requestId, String browser, String detail) {
        jdbcTemplate.update("""
                INSERT INTO sys_audit_log (operate_time, username, module_code, module_name, action_type, action_name, content, target_name, ip_address, status, cost_ms, request_id, browser, detail)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, operateTime, username, moduleCode, moduleName, actionType, actionName, content, targetName, ipAddress, status, costMs, requestId, browser, detail);
    }
}
