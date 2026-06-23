package com.qws.crm;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
@DependsOn("crmCustomerDataInitializer")
public class CrmContactDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public CrmContactDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seedContacts();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_contact (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(50) NOT NULL,
                    title VARCHAR(50),
                    customer_id BIGINT,
                    customer_name VARCHAR(100),
                    phone VARCHAR(50),
                    email VARCHAR(100),
                    is_primary TINYINT DEFAULT 0,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_contact_name (name),
                    INDEX idx_contact_customer (customer_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedContacts() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_contact", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        // {姓名, 职位, 客户名称, 联系电话, 电子邮箱, 是否主联系人, 创建时间}
        Object[][] seeds = new Object[][]{
                {"张三", "采购经理", "北京科技有限公司", "13800138001", "zhangsan@bjtech.com", 1, LocalDateTime.of(2026, 5, 1, 9, 0)},
                {"李四", "技术总监", "上海贸易有限公司", "13800138002", "lisi@shmy.com", 1, LocalDateTime.of(2026, 5, 5, 9, 0)},
                {"王五", "采购专员", "广州电子公司", "13800138003", "wangwu@gzdz.com", 0, LocalDateTime.of(2026, 5, 10, 9, 0)},
                {"赵六", "CEO", "深圳半导体公司", "13800138004", "zhaoliu@szbd.com", 1, LocalDateTime.of(2026, 5, 15, 9, 0)},
                {"钱七", "项目经理", "武汉科技公司", "13800138005", "qianqi@whkj.com", 0, LocalDateTime.of(2026, 5, 20, 9, 0)}
        };
        for (Object[] row : seeds) {
            Long customerId = jdbcTemplate.query(
                    "SELECT id FROM crm_customer WHERE name = ? LIMIT 1",
                    rs -> rs.next() ? rs.getLong("id") : null, row[2]);
            jdbcTemplate.update("""
                    INSERT INTO crm_contact
                    (name, title, customer_id, customer_name, phone, email, is_primary, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], customerId, row[2], row[3], row[4], row[5], row[6], row[6]);
        }
    }
}
