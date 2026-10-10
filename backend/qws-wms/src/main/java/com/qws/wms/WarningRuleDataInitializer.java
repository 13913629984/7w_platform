package com.qws.wms;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 初始化库存预警规则表（wms_warning_rule）。
 */
@Component
public class WarningRuleDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public WarningRuleDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_warning_rule (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    scope VARCHAR(20) NOT NULL DEFAULT 'global',
                    material_id BIGINT NULL,
                    warehouse_code VARCHAR(50) NULL,
                    low_threshold INT NOT NULL DEFAULT 0,
                    medium_threshold INT NOT NULL DEFAULT 0,
                    high_threshold INT NOT NULL DEFAULT 0,
                    enabled TINYINT NOT NULL DEFAULT 1,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_scope (scope),
                    INDEX idx_material (material_id),
                    INDEX idx_warehouse (warehouse_code),
                    INDEX idx_enabled (enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
