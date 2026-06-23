package com.qws.wms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class MaterialDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public MaterialDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedMaterials();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_material (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    sku VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    spec VARCHAR(100),
                    category VARCHAR(50) NOT NULL,
                    brand VARCHAR(50),
                    unit VARCHAR(20) NOT NULL,
                    unit_price DECIMAL(12,2) DEFAULT 0,
                    safe_stock INT DEFAULT 0,
                    supplier VARCHAR(100),
                    barcode VARCHAR(50),
                    shelf_life_days INT,
                    weight DECIMAL(10,3),
                    volume DECIMAL(10,3),
                    remark VARCHAR(500),
                    status TINYINT DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_category (category),
                    INDEX idx_brand (brand),
                    INDEX idx_name (name),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedMaterials() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_material", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"MAT-EL-001", "工业控制主板", "IPC-MB-8G", "electronics", "西门子", "件", 1280.00, 120, "上海智造", "6901234567001", 1825, 0.450, 0.300, "工业现场总线主板，8G内存", 1},
                {"MAT-EL-002", "通信模块", "RS485", "electronics", "西门子", "件", 480.00, 100, "上海智造", "6901234567002", 1825, 0.150, 0.100, "RS485工业通信模块", 1},
                {"MAT-EL-003", "电源模块", "DR-24V-5A", "electronics", "明纬", "件", 220.00, 80, "明纬电子", "6901234567003", 1825, 0.380, 0.220, "24V/5A导轨电源", 1},
                {"MAT-HW-001", "不锈钢螺栓", "M8*35", "hardware", "固特", "盒", 86.00, 300, "苏州精工", "6901234567004", null, 4.500, 0.012, "304不锈钢内六角螺栓", 1},
                {"MAT-HW-002", "镀锌垫片", "M8", "hardware", "固特", "盒", 22.00, 400, "苏州精工", "6901234567005", null, 2.300, 0.005, "GB95镀锌平垫片", 1},
                {"MAT-HW-003", "钣金机箱", "ATX-280", "hardware", "七维", "台", 320.00, 60, "杭州七维", "6901234567006", null, 6.800, 28.000, "标准ATX钣金机箱", 1},
                {"MAT-RB-001", "密封圈", "Φ38 NBR", "rubber", "耐驰", "个", 3.50, 500, "耐驰橡胶", "6901234567007", 1095, 0.012, 0.002, "丁腈橡胶密封圈，耐油", 1},
                {"MAT-RB-002", "减震垫", "60*60*10", "rubber", "耐驰", "个", 8.00, 200, "耐驰橡胶", "6901234567008", 1095, 0.080, 0.040, "橡胶减震垫 高弹性", 1},
                {"MAT-RW-001", "冷轧钢板", "1.2*1250", "raw", "宝钢", "吨", 5280.00, 5, "宝钢直供", "6901234567009", null, 1000.000, 1000.000, "冷轧钢板 SPCC", 1},
                {"MAT-FG-001", "成品控制柜", "QW-900", "finished", "七维", "台", 18600.00, 20, "本厂自产", "6901234567010", 1825, 65.000, 180.000, "九维标准控制柜", 1},
                {"MAT-FG-002", "智能采集器", "QW-IO8", "finished", "七维", "台", 3680.00, 35, "本厂自产", "6901234567011", 1825, 1.200, 3.500, "8通道智能数据采集器", 0}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO wms_material
                    (sku, name, spec, category, brand, unit, unit_price, safe_stock, supplier, barcode,
                     shelf_life_days, weight, volume, remark, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9],
                    row[10], row[11], row[12], row[13], row[14], LocalDateTime.now(), LocalDateTime.now());
        }
    }
}
