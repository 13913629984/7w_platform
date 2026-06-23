package com.qws.crm;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@DependsOn("crmCustomerDataInitializer")
public class CrmDealDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public CrmDealDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seedDeals();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_deal (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    customer_id BIGINT,
                    customer_name VARCHAR(100),
                    lead_name VARCHAR(100),
                    amount DECIMAL(12,2) DEFAULT 0,
                    stage VARCHAR(30),
                    win_rate INT,
                    owner VARCHAR(50),
                    expect_deal_at DATE,
                    status VARCHAR(20) DEFAULT '进行中',
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_deal_name (name),
                    INDEX idx_deal_stage (stage),
                    INDEX idx_deal_owner (owner)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedDeals() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_deal", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        // {商机名称, 客户名称, 关联线索, 金额(万), 阶段, 胜率, 负责人, 预计成交}
        Object[][] seeds = new Object[][]{
                {"华为智能终端外壳批量供应", "华为技术有限公司", "成都软件公司", 580, "即将签约", 75, "刘洋", LocalDate.of(2026, 6, 30)},
                {"小米智能家居主板采购", "小米科技有限公司", null, 320, "方案报价", 60, "刘洋", LocalDate.of(2026, 7, 15)},
                {"比亚迪电池管理系统", "比亚迪股份有限公司", null, 860, "商务谈判", 80, "陈静", LocalDate.of(2026, 6, 20)},
                {"大疆无人机结构件", "大疆创新科技有限公司", null, 150, "需求确认", 40, "刘洋", LocalDate.of(2026, 8, 1)},
                {"迈瑞医疗监护仪配件", "深圳迈瑞医疗电子", null, 220, "方案报价", 55, "陈静", LocalDate.of(2026, 7, 30)},
                {"宁德时代储能柜结构件", "宁德时代新能源", null, 1200, "即将签约", 70, "刘洋", LocalDate.of(2026, 9, 1)}
        };
        for (Object[] row : seeds) {
            Long customerId = jdbcTemplate.query(
                    "SELECT id FROM crm_customer WHERE name = ? LIMIT 1",
                    rs -> rs.next() ? rs.getLong("id") : null, row[1]);
            jdbcTemplate.update("""
                    INSERT INTO crm_deal
                    (name, customer_id, customer_name, lead_name, amount, stage, win_rate, owner, expect_deal_at, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '进行中', ?, ?)
                    """, row[0], customerId, row[1], row[2], row[3], row[4], row[5], row[6], row[7],
                    LocalDateTime.now(), LocalDateTime.now());
        }
    }
}
