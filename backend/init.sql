CREATE DATABASE IF NOT EXISTS 7wsys DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE 7wsys;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '����ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '�û���',
    password VARCHAR(255) NOT NULL COMMENT '����',
    nickname VARCHAR(50) COMMENT '�ǳ�',
    email VARCHAR(100) COMMENT '����',
    phone VARCHAR(20) COMMENT '�ֻ���',
    employee_id VARCHAR(50) COMMENT '����',
    department VARCHAR(100) COMMENT '����',
    position VARCHAR(100) COMMENT '��λ',
    role VARCHAR(50) COMMENT '��ɫ',
    status TINYINT DEFAULT 1 COMMENT '״̬��0-���ã�1-����',
    last_login DATETIME COMMENT '����¼ʱ��',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '����ʱ��',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '����ʱ��',
    INDEX idx_username (username),
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ϵͳ�û���';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织部门表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织员工表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    type VARCHAR(20) NOT NULL DEFAULT 'custom' COMMENT '角色类型：system/custom',
    description VARCHAR(500) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    code VARCHAR(80) NOT NULL UNIQUE COMMENT '权限编码',
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    parent_code VARCHAR(80) COMMENT '父权限编码',
    module VARCHAR(50) COMMENT '所属模块',
    description VARCHAR(500) COMMENT '权限描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_code (parent_code),
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    permission_code VARCHAR(80) NOT NULL COMMENT '权限编码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_code, permission_code),
    INDEX idx_role_code (role_code),
    INDEX idx_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_code),
    INDEX idx_user_id (user_id),
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

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
