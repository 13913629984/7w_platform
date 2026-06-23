package com.qws.wms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
public class WarehouseDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public WarehouseDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seed();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_warehouse (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    type VARCHAR(30) NOT NULL,
                    address VARCHAR(200),
                    manager VARCHAR(50),
                    phone VARCHAR(50),
                    area DECIMAL(12,2) DEFAULT 0,
                    capacity DECIMAL(12,2) DEFAULT 0,
                    used_capacity DECIMAL(12,2) DEFAULT 0,
                    description VARCHAR(500),
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_type (type),
                    INDEX idx_status (status),
                    INDEX idx_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_warehouse", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"WH-A", "主仓库-A区", "raw", "杭州市余杭区七维产业园1号仓", "张伟", "13800000001", 1800.00, 12000.00, 8200.00, "原材料与电子件主存储区", 10, 1},
                {"WH-B", "主仓库-B区", "raw", "杭州市余杭区七维产业园2号仓", "李明", "13800000002", 1500.00, 9500.00, 6100.00, "五金件、橡胶件及常规物料区", 20, 1},
                {"WH-FG", "成品仓", "finished", "杭州市余杭区七维产业园3号仓", "王芳", "13800000003", 2200.00, 16000.00, 7300.00, "成品控制柜与智能采集器存储区", 30, 1},
                {"WH-SP", "备品备件仓", "spare", "杭州市余杭区七维产业园备件中心", "赵强", "13800000004", 900.00, 5000.00, 1800.00, "维修备件与低值易耗品仓库", 40, 1},
                {"WH-TMP", "检验暂存仓", "temporary", "杭州市余杭区七维质检中心", "陈晨", "13800000005", 600.00, 2500.00, 600.00, "来料检验与不合格暂存区", 50, 1},
                {"WH-OLD", "旧仓库", "normal", "老厂区北侧仓库", "刘敏", "13800000006", 500.00, 1800.00, 0.00, "历史仓库，待停用迁移", 60, 0}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO wms_warehouse
                    (code, name, type, address, manager, phone, area, capacity, used_capacity, description, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10], row[11], LocalDateTime.now(), LocalDateTime.now());
        }
    }
}