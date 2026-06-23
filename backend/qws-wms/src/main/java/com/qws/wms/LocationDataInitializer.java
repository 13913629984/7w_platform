package com.qws.wms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
public class LocationDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public LocationDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seed();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_location (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    warehouse_code VARCHAR(50) NOT NULL,
                    area VARCHAR(50),
                    shelf VARCHAR(50),
                    layer VARCHAR(50),
                    position VARCHAR(50),
                    type VARCHAR(30) NOT NULL,
                    capacity DECIMAL(12,2) DEFAULT 0,
                    used_capacity DECIMAL(12,2) DEFAULT 0,
                    description VARCHAR(500),
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_warehouse (warehouse_code),
                    INDEX idx_type (type),
                    INDEX idx_status (status),
                    INDEX idx_area_shelf (area, shelf)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_location", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"A-01-001", "A区01架001位", "WH-A", "A区", "01架", "1层", "001", "shelf", 1000.00, 620.00, "电子件常规库位", 10, 1},
                {"A-01-002", "A区01架002位", "WH-A", "A区", "01架", "1层", "002", "shelf", 1000.00, 450.00, "电子件常规库位", 20, 1},
                {"A-02-001", "A区02架001位", "WH-A", "A区", "02架", "1层", "001", "pallet", 1200.00, 900.00, "托盘位，适合控制主板箱", 30, 1},
                {"B-01-001", "B区01架001位", "WH-B", "B区", "01架", "1层", "001", "shelf", 800.00, 300.00, "五金件库位", 40, 1},
                {"B-02-001", "B区02架001位", "WH-B", "B区", "02架", "1层", "001", "floor", 1500.00, 1100.00, "大件钣金地堆位", 50, 1},
                {"FG-01-001", "成品区01位", "WH-FG", "成品区", "01排", "1层", "001", "pallet", 2000.00, 860.00, "控制柜成品托盘位", 60, 1},
                {"FG-01-002", "成品区02位", "WH-FG", "成品区", "01排", "1层", "002", "pallet", 2000.00, 0.00, "空闲成品位", 70, 1},
                {"SP-01-001", "备件01位", "WH-SP", "备件区", "01架", "1层", "001", "shelf", 500.00, 130.00, "维修备件库位", 80, 1},
                {"TMP-01-001", "检验暂存01位", "WH-TMP", "暂存区", "01排", "1层", "001", "temporary", 600.00, 260.00, "来料检验暂存", 90, 1},
                {"OLD-01-001", "旧仓库01位", "WH-OLD", "旧仓区", "01排", "1层", "001", "floor", 500.00, 0.00, "旧库空闲位", 100, 0}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO wms_location
                    (code, name, warehouse_code, area, shelf, layer, position, type, capacity, used_capacity, description, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10], row[11], row[12], LocalDateTime.now(), LocalDateTime.now());
        }
    }
}