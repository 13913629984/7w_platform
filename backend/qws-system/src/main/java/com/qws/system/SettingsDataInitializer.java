package com.qws.system;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 系统设置：参数配置与数据字典的建表 + 种子数据。 */
@Component
public class SettingsDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public SettingsDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedConfigs();
        seedDicts();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_config (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    config_key VARCHAR(80) NOT NULL UNIQUE,
                    config_name VARCHAR(100) NOT NULL,
                    config_value VARCHAR(500),
                    value_type VARCHAR(20) NOT NULL DEFAULT 'text',
                    options VARCHAR(500),
                    config_group VARCHAR(50) NOT NULL DEFAULT '通用',
                    description VARCHAR(500),
                    is_system TINYINT NOT NULL DEFAULT 0,
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_config_group (config_group),
                    INDEX idx_config_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_dict_type (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    dict_code VARCHAR(80) NOT NULL UNIQUE,
                    dict_name VARCHAR(100) NOT NULL,
                    description VARCHAR(500),
                    is_system TINYINT NOT NULL DEFAULT 0,
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_dict_code (dict_code),
                    INDEX idx_dict_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_dict_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    dict_code VARCHAR(80) NOT NULL,
                    item_value VARCHAR(100) NOT NULL,
                    item_label VARCHAR(100) NOT NULL,
                    color VARCHAR(20) DEFAULT 'primary',
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_item_dict_code (dict_code),
                    INDEX idx_item_value (item_value)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedConfigs() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_config", Integer.class);
        if (count != null && count > 0) return;

        // {key, 名称, 值, 类型, 选项, 分组, 说明, 是否内置, 排序}
        Object[][] seeds = new Object[][]{
                {"sys.name", "系统名称", "七维空间信息化平台", "text", null, "基础设置", "显示在登录页与页面标题", 1, 10},
                {"sys.short_name", "系统简称", "QWS", "text", null, "基础设置", "用于侧边栏折叠态与浏览器标签", 1, 20},
                {"sys.copyright", "版权信息", "© 2026 七维空间科技", "text", null, "基础设置", "显示在页面底部", 1, 30},
                {"sys.session_timeout", "会话超时（分钟）", "120", "number", null, "基础设置", "JWT 有效期，修改后需重新登录生效", 1, 40},
                {"sys.page_size", "默认分页条数", "10", "select", "10,20,50,100", "基础设置", "列表页默认每页展示条数", 1, 50},
                {"sys.theme", "默认主题", "blue", "select", "blue,green,dark", "外观设置", "登录后默认配色方案", 0, 10},
                {"sys.sidebar_collapsed", "侧边栏默认收起", "false", "boolean", null, "外观设置", "登录后侧边栏是否折叠", 0, 20},
                {"sys.audit_enabled", "启用操作审计", "true", "boolean", null, "安全设置", "关闭后不再写入审计日志", 1, 10},
                {"sys.audit_retention_days", "审计日志保留天数", "90", "number", null, "安全设置", "超期日志可归档或清理", 0, 20},
                {"sys.login_captcha", "登录验证码", "false", "boolean", null, "安全设置", "是否开启图形验证码", 0, 30},
                {"sys.password_min_length", "密码最小长度", "6", "number", null, "安全设置", "新建用户时的密码校验规则", 1, 40},
                {"sys.upload_max_mb", "附件上传上限（MB）", "20", "number", null, "文件设置", "单个文件大小上限", 0, 10},
                {"sys.export_max_rows", "导出最大行数", "10000", "number", null, "文件设置", "超出后提示缩小查询范围", 0, 20},
                {"sys.notify_enabled", "启用消息通知", "true", "boolean", null, "通知设置", "工单、预警等业务提醒开关", 0, 10}
        };

        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 9, 0);
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO sys_config
                    (config_key, config_name, config_value, value_type, options, config_group,
                     description, is_system, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], now, now);
        }
    }

    private void seedDicts() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_dict_type", Integer.class);
        if (count != null && count > 0) return;

        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 9, 0);

        // 字典类型：{code, 名称, 说明, 是否内置, 排序}
        Object[][] types = new Object[][]{
                {"maint_equipment_status", "设备状态", "维保-设备台账状态", 0, 10},
                {"maint_work_order_status", "工单状态", "维保-维修工单流转状态", 0, 20},
                {"maint_work_order_priority", "工单优先级", "维保-工单紧急程度", 0, 30},
                {"maint_work_order_type", "工单类型", "维保-工单业务类型", 0, 40},
                {"maint_plan_cycle", "保养周期", "维保-保养计划周期单位", 0, 50},
                {"ets_register_status", "来货登记状态", "电测-登记单状态", 0, 60},
                {"ets_test_result", "测试结果", "电测-测试判定结果", 0, 70},
                {"ets_report_status", "报告状态", "电测-测试报告状态", 0, 80},
                {"crm_lead_status", "线索状态", "CRM-销售线索状态", 0, 90},
                {"crm_lead_source", "线索来源", "CRM-线索获取渠道", 0, 100},
                {"crm_activity_type", "跟进方式", "CRM-客户跟进活动类型", 0, 110}
        };
        for (Object[] t : types) {
            jdbcTemplate.update("""
                    INSERT INTO sys_dict_type
                    (dict_code, dict_name, description, is_system, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?)
                    """, t[0], t[1], t[2], t[3], t[4], now, now);
        }

        // 字典项：{dictCode, value, label, color, sort}
        Object[][] items = new Object[][]{
                {"maint_equipment_status", "running", "运行中", "success", 10},
                {"maint_equipment_status", "idle", "闲置", "info", 20},
                {"maint_equipment_status", "repair", "维修中", "warning", 30},
                {"maint_equipment_status", "scrapped", "已报废", "danger", 40},

                {"maint_work_order_status", "pending", "待派单", "info", 10},
                {"maint_work_order_status", "assigned", "已派单", "primary", 20},
                {"maint_work_order_status", "processing", "处理中", "warning", 30},
                {"maint_work_order_status", "completed", "已完成", "success", 40},
                {"maint_work_order_status", "closed", "已关闭", "info", 50},

                {"maint_work_order_priority", "low", "低", "info", 10},
                {"maint_work_order_priority", "normal", "普通", "primary", 20},
                {"maint_work_order_priority", "high", "高", "warning", 30},
                {"maint_work_order_priority", "urgent", "紧急", "danger", 40},

                {"maint_work_order_type", "repair", "维修", "danger", 10},
                {"maint_work_order_type", "preventive", "保养", "success", 20},
                {"maint_work_order_type", "install", "安装", "primary", 30},
                {"maint_work_order_type", "inspection", "巡检", "info", 40},

                {"maint_plan_cycle", "day", "天", "primary", 10},
                {"maint_plan_cycle", "week", "周", "primary", 20},
                {"maint_plan_cycle", "month", "月", "primary", 30},
                {"maint_plan_cycle", "quarter", "季", "primary", 40},
                {"maint_plan_cycle", "year", "年", "primary", 50},

                {"ets_register_status", "pending", "待收样", "info", 10},
                {"ets_register_status", "received", "已收样", "primary", 20},
                {"ets_register_status", "testing", "测试中", "warning", 30},
                {"ets_register_status", "done", "已完成", "success", 40},
                {"ets_register_status", "returned", "已退回", "danger", 50},

                {"ets_test_result", "pass", "合格", "success", 10},
                {"ets_test_result", "fail", "不合格", "danger", 20},
                {"ets_test_result", "pending", "待测试", "info", 30},

                {"ets_report_status", "draft", "草稿", "info", 10},
                {"ets_report_status", "pending", "待审核", "warning", 20},
                {"ets_report_status", "approved", "已通过", "success", 30},
                {"ets_report_status", "rejected", "已驳回", "danger", 40},

                {"crm_lead_status", "待跟进", "待跟进", "info", 10},
                {"crm_lead_status", "跟进中", "跟进中", "primary", 20},
                {"crm_lead_status", "已转化", "已转化", "success", 30},
                {"crm_lead_status", "已失效", "已失效", "danger", 40},

                {"crm_lead_source", "官网咨询", "官网咨询", "primary", 10},
                {"crm_lead_source", "行业展会", "行业展会", "success", 20},
                {"crm_lead_source", "客户转介绍", "客户转介绍", "warning", 30},
                {"crm_lead_source", "电话拜访", "电话拜访", "info", 40},

                {"crm_activity_type", "visit", "拜访", "primary", 10},
                {"crm_activity_type", "call", "电话", "success", 20},
                {"crm_activity_type", "meeting", "会议", "warning", 30},
                {"crm_activity_type", "email", "邮件", "info", 40},
                {"crm_activity_type", "other", "其他", "info", 50}
        };
        for (Object[] i : items) {
            jdbcTemplate.update("""
                    INSERT INTO sys_dict_item
                    (dict_code, item_value, item_label, color, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?)
                    """, i[0], i[1], i[2], i[3], i[4], now, now);
        }
    }
}
