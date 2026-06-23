package com.qws.crm;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@DependsOn({"crmCustomerDataInitializer", "crmDealDataInitializer"})
public class CrmOrderDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public CrmOrderDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedOrders();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_order (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_no VARCHAR(40) NOT NULL,
                    customer_id BIGINT,
                    customer_name VARCHAR(100),
                    deal_id BIGINT,
                    deal_name VARCHAR(100),
                    contract_no VARCHAR(60),
                    amount DECIMAL(12,2) DEFAULT 0,
                    owner VARCHAR(50),
                    sign_date DATE,
                    expect_deliver_at DATE,
                    status VARCHAR(20) DEFAULT '待确认',
                    remark VARCHAR(500),
                    outbound_code VARCHAR(40),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_order_no (order_no),
                    INDEX idx_order_customer (customer_id),
                    INDEX idx_order_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_order_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    product_code VARCHAR(60),
                    product_name VARCHAR(120),
                    spec VARCHAR(80),
                    quantity INT DEFAULT 0,
                    price DECIMAL(12,2) DEFAULT 0,
                    subtotal DECIMAL(12,2) DEFAULT 0,
                    INDEX idx_item_order (order_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        ensureOutboundCodeColumn();
    }

    /** 兼容历史已建表：补充 outbound_code 列（用于关联出库单 / 幂等判断） */
    private void ensureOutboundCodeColumn() {
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_order' AND COLUMN_NAME = 'outbound_code'
                """, Integer.class);
        if (exists == null || exists == 0) {
            jdbcTemplate.execute("ALTER TABLE crm_order ADD COLUMN outbound_code VARCHAR(40)");
        }
    }

    private void seedOrders() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_order", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        // {订单编号, 客户名称, 商机名称, 金额(万), 合同编号, 签订日期, 预计发货, 状态, 负责人}
        Object[][] seeds = new Object[][]{
                {"SO20260528001", "华为技术有限公司", "华为智能终端外壳批量供应", 580, "HT20260528001", LocalDate.of(2026, 5, 28), LocalDate.of(2026, 6, 5), "已确认", "刘洋"},
                {"SO20260527001", "比亚迪股份有限公司", "比亚迪电池管理系统", 860, "HT20260527001", LocalDate.of(2026, 5, 27), LocalDate.of(2026, 6, 10), "已发货", "陈静"},
                {"SO20260526001", "宁德时代新能源", "宁德时代储能柜结构件", 1200, "HT20260526001", LocalDate.of(2026, 5, 26), LocalDate.of(2026, 6, 15), "待确认", "刘洋"},
                {"SO20260525001", "小米科技有限公司", "小米智能家居主板采购", 320, "HT20260525001", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 1), "已完成", "刘洋"},
                {"SO20260524001", "大疆创新科技有限公司", "大疆无人机结构件", 150, "HT20260524001", LocalDate.of(2026, 5, 24), LocalDate.of(2026, 5, 30), "已完成", "刘洋"}
        };
        for (Object[] row : seeds) {
            Long customerId = jdbcTemplate.query(
                    "SELECT id FROM crm_customer WHERE name = ? LIMIT 1",
                    rs -> rs.next() ? rs.getLong("id") : null, row[1]);
            Long dealId = jdbcTemplate.query(
                    "SELECT id FROM crm_deal WHERE name = ? LIMIT 1",
                    rs -> rs.next() ? rs.getLong("id") : null, row[2]);
            jdbcTemplate.update("""
                    INSERT INTO crm_order
                    (order_no, customer_id, customer_name, deal_id, deal_name, contract_no, amount, owner, sign_date, expect_deliver_at, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], customerId, row[1], dealId, row[2], row[4], row[3], row[8], row[5], row[6], row[7],
                    LocalDateTime.now(), LocalDateTime.now());
            Long orderId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM crm_order", Long.class);
            // 单条产品明细，单价=订单金额、数量=1，行合计=订单金额
            jdbcTemplate.update("""
                    INSERT INTO crm_order_item
                    (order_id, product_code, product_name, spec, quantity, price, subtotal)
                    VALUES (?, ?, ?, ?, 1, ?, ?)
                    """, orderId, "P" + row[0], row[2], "标准规格", row[3], row[3]);
        }
    }
}
