package com.qws.wms;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WarningDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public WarningDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_stock_warning (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    material_id BIGINT NOT NULL,
                    warehouse_code VARCHAR(50) NOT NULL,
                    current_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    threshold INT NOT NULL DEFAULT 0,
                    level VARCHAR(20) NOT NULL DEFAULT 'low',
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    handler VARCHAR(60),
                    handle_remark VARCHAR(500),
                    handle_time DATETIME,
                    warning_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_warning_material_wh (material_id, warehouse_code),
                    INDEX idx_warehouse (warehouse_code),
                    INDEX idx_level (level),
                    INDEX idx_status (status),
                    INDEX idx_warning_time (warning_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
