package com.qws.crm;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class CrmCustomerDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public CrmCustomerDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedCustomers();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_customer (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    english_name VARCHAR(120),
                    address VARCHAR(200),
                    level VARCHAR(20),
                    owner VARCHAR(50),
                    last_visit_at DATE,
                    last_deal_at DATE,
                    remark VARCHAR(500),
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    INDEX idx_level (level),
                    INDEX idx_owner (owner)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_customer_sales (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_id BIGINT NOT NULL,
                    name VARCHAR(50),
                    position VARCHAR(50),
                    is_owner TINYINT DEFAULT 0,
                    INDEX idx_sales_customer (customer_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_customer_contact (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_id BIGINT NOT NULL,
                    name VARCHAR(50),
                    phone VARCHAR(50),
                    title VARCHAR(50),
                    INDEX idx_contact_customer (customer_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedCustomers() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_customer", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"北京科技有限公司", "Beijing Technology Co., Ltd.", "北京市海淀区中关村大街1号", "A级", "张三", LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 25), "销售经理", "采购总监", "陈总", "13800000001"},
                {"上海贸易有限公司", "Shanghai Trading Co., Ltd.", "上海市浦东新区世纪大道100号", "A级", "李四", LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 20), "客户经理", "采购经理", "王经理", "13800000002"},
                {"广州电子公司", "Guangzhou Electronics", "广州市天河区珠江新城", "B级", "王五", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 18), "销售代表", "技术总监", "刘工", "13800000003"},
                {"深圳半导体公司", "Shenzhen Semiconductor", "深圳市南山区科技园", "A级", "赵六", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 28), "销售经理", "采购主管", "孙经理", "13800000004"},
                {"武汉科技公司", "Wuhan Technology", "武汉市东湖高新区光谷大道", "B级", "钱七", LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 15), "销售代表", "运营总监", "周总", "13800000005"}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO crm_customer
                    (name, english_name, address, level, owner, last_visit_at, last_deal_at, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], LocalDateTime.now(), LocalDateTime.now());
            Long customerId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM crm_customer", Long.class);
            jdbcTemplate.update("""
                    INSERT INTO crm_customer_sales (customer_id, name, position, is_owner)
                    VALUES (?, ?, ?, 1)
                    """, customerId, row[4], row[7]);
            jdbcTemplate.update("""
                    INSERT INTO crm_customer_contact (customer_id, name, phone, title)
                    VALUES (?, ?, ?, ?)
                    """, customerId, row[9], row[10], row[8]);
        }
        seedPublicCustomers();
    }

    // 公海客户：未分配销售负责人（owner 为空），仅生成客户与联系人，不生成销售记录
    private void seedPublicCustomers() {
        Object[][] publicSeeds = new Object[][]{
                {"晨希仪器", "", "北京市石景山区", "C级", "龙文燕", "13800000010"},
                {"江西金普通信信息技术有限公司", "", "江西省南昌市新建区玉屏东大街299号", "C级", "张总", "13800000011"},
                {"重庆中环智能科技有限公司", "", "重庆市江津区祥瑞大道619号", "C级", "浩工", "13800000012"},
                {"昆山奥鼎精密模具有限公司", "", "江苏省苏州市昆山市", "C级", "袁柠", "13800000013"},
                {"南通斯伯德机器人", "", "江苏省苏州市虎丘区东富路55号", "C级", "宋总", "13800000014"}
        };
        for (Object[] row : publicSeeds) {
            jdbcTemplate.update("""
                    INSERT INTO crm_customer
                    (name, english_name, address, level, owner, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, '', 1, ?, ?)
                    """, row[0], row[1], row[2], row[3], LocalDateTime.now(), LocalDateTime.now());
            Long customerId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM crm_customer", Long.class);
            jdbcTemplate.update("""
                    INSERT INTO crm_customer_contact (customer_id, name, phone, title)
                    VALUES (?, ?, ?, '联系人')
                    """, customerId, row[4], row[5]);
        }
    }
}
