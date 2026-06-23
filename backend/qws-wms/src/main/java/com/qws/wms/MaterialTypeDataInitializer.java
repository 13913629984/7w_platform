package com.qws.wms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
public class MaterialTypeDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public MaterialTypeDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seed();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_material_type (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    description VARCHAR(500),
                    parent_code VARCHAR(50),
                    level INT NOT NULL DEFAULT 1,
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_parent (parent_code),
                    INDEX idx_level (level),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_material_type", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"raw", "原材料", "生产所需基础原材料", null, 1, 10, 1},
                {"electronics", "电子件", "电路板、模块、线束等电子类物料", null, 1, 20, 1},
                {"hardware", "五金件", "螺丝、支架、钣金件等", null, 1, 30, 1},
                {"rubber", "橡胶件", "密封圈、减震垫等橡胶物料", null, 1, 40, 1},
                {"finished", "成品", "已完成生产并可销售的产品", null, 1, 50, 1},
                {"electronics-ic", "集成电路", "IC、芯片、控制器等", "electronics", 2, 21, 1},
                {"electronics-conn", "连接器", "端子、接插件、连接线束", "electronics", 2, 22, 1},
                {"hardware-screw", "标准紧固件", "螺丝、垫片、螺母等", "hardware", 2, 31, 1},
                {"hardware-sheet", "钣金件", "机箱、面板、钣金加工件", "hardware", 2, 32, 1}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO wms_material_type (code, name, description, parent_code, level, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], LocalDateTime.now(), LocalDateTime.now());
        }
    }
}