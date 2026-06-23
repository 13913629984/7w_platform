package com.qws.wms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
public class BrandDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public BrandDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seed();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_brand (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    english_name VARCHAR(100),
                    logo VARCHAR(255),
                    country VARCHAR(50),
                    supplier VARCHAR(100),
                    contact VARCHAR(50),
                    phone VARCHAR(50),
                    description VARCHAR(500),
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_brand", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"BR-QW", "七维", "QWS", "中国", "杭州七维", "王工", "0571-88880001", "自有品牌，覆盖控制柜与智能采集器", 10, 1},
                {"BR-SIEMENS", "西门子", "Siemens", "德国", "上海智造", "李经理", "021-80000001", "工业自动化与电子控制元件品牌", 20, 1},
                {"BR-MW", "明纬", "Mean Well", "中国台湾", "明纬电子", "陈经理", "021-80000002", "电源模块与工业电源品牌", 30, 1},
                {"BR-GT", "固特", "Goodfix", "中国", "苏州精工", "张经理", "0512-80000003", "五金紧固件品牌", 40, 1},
                {"BR-NC", "耐驰", "Nachi", "中国", "耐驰橡胶", "赵经理", "0512-80000004", "橡胶密封与减震件品牌", 50, 1},
                {"BR-BG", "宝钢", "Baosteel", "中国", "宝钢直供", "刘经理", "021-80000005", "钢板及金属原材料品牌", 60, 1}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO wms_brand
                    (code, name, english_name, country, supplier, contact, phone, description, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], LocalDateTime.now(), LocalDateTime.now());
        }
    }
}