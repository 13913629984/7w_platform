package com.qws.crm;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@DependsOn("crmCustomerDataInitializer")
public class CrmWorkspaceDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public CrmWorkspaceDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedLeads();
        seedActivities();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_lead (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    source VARCHAR(50),
                    owner VARCHAR(50),
                    status VARCHAR(20) DEFAULT '待跟进',
                    follow_at DATE,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_lead_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_activity (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(120) NOT NULL,
                    type VARCHAR(30),
                    customer_id BIGINT,
                    customer_name VARCHAR(100),
                    owner VARCHAR(50),
                    activity_at DATETIME,
                    status VARCHAR(20) DEFAULT '待处理',
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_activity_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedLeads() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_lead", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        // {线索名称, 来源, 跟进日期, 负责人}
        Object[][] seeds = new Object[][]{
                {"广州智能制造公司", "官网咨询", LocalDate.of(2026, 5, 28), "刘洋"},
                {"杭州物联网科技", "电话营销", LocalDate.of(2026, 5, 28), "陈静"},
                {"成都软件公司", "客户推荐", LocalDate.of(2026, 5, 27), "刘洋"},
                {"西安电子科技", "行业展会", LocalDate.of(2026, 5, 27), "陈静"},
                {"南京科技集团", "官网咨询", LocalDate.of(2026, 5, 26), "刘洋"}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO crm_lead (name, source, follow_at, owner, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, '待跟进', ?, ?)
                    """, row[0], row[1], row[2], row[3], LocalDateTime.now(), LocalDateTime.now());
        }
    }

    private void seedActivities() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_activity", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        // {标题, 类型, 客户名称, 活动时间, 负责人}
        Object[][] seeds = new Object[][]{
                {"客户回访 - 北京科技", "回访", "北京科技有限公司", LocalDateTime.of(2026, 5, 28, 14, 0), "刘洋"},
                {"产品演示 - 上海贸易", "演示", "上海贸易有限公司", LocalDateTime.of(2026, 5, 29, 10, 0), "陈静"},
                {"合同谈判 - 深圳半导体", "谈判", "深圳半导体公司", LocalDateTime.of(2026, 5, 30, 15, 0), "刘洋"},
                {"技术交流 - 广州电子", "交流", "广州电子公司", LocalDateTime.of(2026, 6, 1, 9, 30), "陈静"}
        };
        for (Object[] row : seeds) {
            Long customerId = jdbcTemplate.query(
                    "SELECT id FROM crm_customer WHERE name = ? LIMIT 1",
                    rs -> rs.next() ? rs.getLong("id") : null, row[2]);
            jdbcTemplate.update("""
                    INSERT INTO crm_activity (title, type, customer_id, customer_name, activity_at, owner, status, create_time)
                    VALUES (?, ?, ?, ?, ?, ?, '待处理', ?)
                    """, row[0], row[1], customerId, row[2], row[3], row[4], LocalDateTime.now());
        }
    }
}
