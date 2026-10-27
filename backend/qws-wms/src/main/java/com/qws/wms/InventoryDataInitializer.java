package com.qws.wms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class InventoryDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public InventoryDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seed();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_inventory (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    material_id BIGINT NOT NULL,
                    warehouse_code VARCHAR(50) NOT NULL,
                    location_code VARCHAR(50) NOT NULL,
                    batch_no VARCHAR(80),
                    quantity DECIMAL(14,3) NOT NULL DEFAULT 0,
                    available_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    locked_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    safe_stock INT DEFAULT 0,
                    production_date DATE,
                    expire_date DATE,
                    remark VARCHAR(500),
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_material (material_id),
                    INDEX idx_warehouse (warehouse_code),
                    INDEX idx_location (location_code),
                    INDEX idx_batch (batch_no),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_inventory", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Map<String, Long> materials = jdbcTemplate.query("SELECT id, sku FROM wms_material", rs -> {
            java.util.Map<String, Long> map = new java.util.HashMap<>();
            while (rs.next()) map.put(rs.getString("sku"), rs.getLong("id"));
            return map;
        });
        Object[][] seeds = new Object[][]{
                {"MAT-EL-001", "WH-A", "A-01-001", "B202606001", 168, 150, 18, 120, "工业控制主板首批库存", 1},
                {"MAT-EL-002", "WH-A", "A-01-002", "B202606002", 96, 90, 6, 100, "通信模块库存低于安全线", 1},
                {"MAT-EL-003", "WH-A", "A-02-001", "B202606003", 45, 40, 5, 80, "电源模块低库存", 1},
                {"MAT-HW-001", "WH-B", "B-01-001", "B202606004", 520, 500, 20, 300, "不锈钢螺栓库存", 1},
                {"MAT-HW-002", "WH-B", "B-01-001", "B202606005", 220, 210, 10, 400, "垫片库存低", 1},
                {"MAT-HW-003", "WH-B", "B-02-001", "B202606006", 58, 50, 8, 60, "钣金机箱接近安全库存", 1},
                {"MAT-RB-001", "WH-B", "B-01-001", "B202606007", 780, 760, 20, 500, "密封圈库存", 1},
                {"MAT-RB-002", "WH-SP", "SP-01-001", "B202606008", 0, 0, 0, 200, "减震垫缺货", 1},
                {"MAT-RW-001", "WH-B", "B-02-001", "B202606009", 8, 8, 0, 5, "冷轧钢板库存", 1},
                {"MAT-FG-001", "WH-FG", "FG-01-001", "B202606010", 24, 20, 4, 20, "成品控制柜库存", 1},
                {"MAT-FG-002", "WH-FG", "FG-01-002", "B202606011", 32, 30, 2, 35, "智能采集器库存低", 1}
        };
        for (Object[] row : seeds) {
            Long materialId = materials.get((String) row[0]);
            if (materialId == null) continue;
            jdbcTemplate.update("""
                    INSERT INTO wms_inventory
                    (material_id, warehouse_code, location_code, batch_no, quantity, available_qty, locked_qty, safe_stock,
                     production_date, expire_date, remark, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, materialId, row[1], row[2], row[3], bd(row[4]), bd(row[5]), bd(row[6]), row[7],
                    LocalDate.now().minusDays(30), LocalDate.now().plusDays(365), row[8], row[9], LocalDateTime.now(), LocalDateTime.now());
        }
    }

    private BigDecimal bd(Object value) {
        return new BigDecimal(String.valueOf(value));
    }
}