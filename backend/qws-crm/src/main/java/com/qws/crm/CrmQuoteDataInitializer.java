package com.qws.crm;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class CrmQuoteDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public CrmQuoteDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seedQuotes();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS crm_quote (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_name VARCHAR(120) NOT NULL,
                    brand VARCHAR(80),
                    model VARCHAR(120),
                    price DECIMAL(12,2),
                    visit_subject VARCHAR(200),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_quote_customer (customer_name),
                    INDEX idx_quote_brand (brand),
                    INDEX idx_quote_model (model)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedQuotes() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crm_quote", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"北京中科宇波科技有限公司", "NI", "NI 9401", 400.00, "2款型号打包."},
                {"北京中科宇波科技有限公司", "NI", "NI 9205", 2200.00, "2款型号打包."},
                {"德鑫隆（苏州）智能装备", "阿黛凯/ATEQ", "F620-05", 39000.00, "阿黛凯气密仪初步需求"},
                {"德鑫隆（苏州）智能装备", "阿黛凯/ATEQ", "F620-05", 39000.00, "阿黛凯气密仪初步需求"}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO crm_quote (customer_name, brand, model, price, visit_subject)
                    VALUES (?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4]);
        }
    }
}
