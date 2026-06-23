CREATE DATABASE IF NOT EXISTS 7wsys DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE 7wsys;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    employee_id VARCHAR(50),
    department VARCHAR(100),
    position VARCHAR(100),
    role VARCHAR(50),
    status TINYINT DEFAULT 1,
    last_login DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    permission_code VARCHAR(80) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_code, permission_code),
    INDEX idx_role_code (role_code),
    INDEX idx_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_code),
    INDEX idx_user_id (user_id),
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_audit_log (operate_time, username, module_code, module_name, action_type, action_name, content, target_name, ip_address, status, cost_ms, request_id, browser, detail)
SELECT * FROM (
    SELECT '2026-05-28 14:32:15', 'admin', 'SYS', '系统', 'LOGIN', '登录', '用户登录系统', '-', '192.168.1.100', 'success', 230, 'req_audit_001', 'Chrome 125.0', '{"action":"login","result":"success"}' UNION ALL
    SELECT '2026-05-28 14:35:22', 'admin', 'SYS', '系统', 'CREATE', '新增', '新增用户：张三 (EMP-2026028)', '用户管理', '192.168.1.100', 'success', 560, 'req_audit_002', 'Chrome 125.0', '{"action":"create_user","data":{"name":"张三","empNo":"EMP-2026028"}}' UNION ALL
    SELECT '2026-05-28 14:40:08', 'admin', 'SYS', '系统', 'UPDATE', '修改', '修改系统参数：会话超时 60→120', '系统设置', '192.168.1.100', 'success', 180, 'req_audit_003', 'Chrome 125.0', '{"action":"update_setting","field":"sessionTimeout","from":60,"to":120}' UNION ALL
    SELECT '2026-05-28 14:42:33', 'zhangmh', 'WMS', 'WMS', 'CREATE', '新增', '创建入库单：RK-2026-0528-001，物料：电子元件一批', '入库单据', '192.168.1.105', 'success', 820, 'req_audit_004', 'Edge 125.0', '{"action":"create_inbound","code":"RK-2026-0528-001"}' UNION ALL
    SELECT '2026-05-28 14:45:12', 'wangjg', 'CRM', 'CRM', 'CREATE', '新增', '新建客户：深圳XX科技有限公司', '客户档案', '192.168.1.108', 'success', 450, 'req_audit_005', 'Chrome 125.0', '{"action":"create_customer","customer":"深圳XX科技有限公司"}' UNION ALL
    SELECT '2026-05-28 14:50:05', 'lixy', 'WMS', 'WMS', 'CREATE', '新增', '创建出库单：CK-2026-0528-002，客户：广州YY电子', '出库单据', '192.168.1.109', 'success', 670, 'req_audit_006', 'Chrome 125.0', '{"action":"create_outbound","code":"CK-2026-0528-002"}'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_audit_log);
