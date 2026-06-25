package com.qws.wms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
public class SupplierDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public SupplierDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTable();
        seed();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_supplier (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    short_name VARCHAR(50),
                    category VARCHAR(30),
                    contact VARCHAR(50),
                    phone VARCHAR(50),
                    email VARCHAR(100),
                    address VARCHAR(255),
                    tax_no VARCHAR(50),
                    bank_account VARCHAR(100),
                    settle_type VARCHAR(30),
                    description VARCHAR(500),
                    sort INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    INDEX idx_category (category),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_supplier", Integer.class);
        if (count == null || count > 0) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"SUP-SHZZ", "上海智造科技有限公司", "上海智造", "raw", "李经理", "021-80000001", "li@shzz.com", "上海市浦东新区张江高科技园区88号", "91310115MA1H0001XA", "工行上海张江支行 6222021001088001", "月结30天", "电子件与控制元件主力供应商", 10, 1},
                {"SUP-MWDZ", "明纬电子（中国）有限公司", "明纬电子", "raw", "陈经理", "021-80000002", "chen@meanwell.com", "广州市黄埔区科学城东路12号", "91440101MA9U0002XB", "招行广州科学城支行 6225880002088002", "月结60天", "工业电源与电源模块供应商", 20, 1},
                {"SUP-SZJG", "苏州精工五金有限公司", "苏州精工", "raw", "张经理", "0512-80000003", "zhang@szjg.com", "苏州市工业园区星湖街200号", "91320594MA1M0003XC", "建行苏州园区支行 6217000003088003", "货到付款", "五金紧固件与结构件供应商", 30, 1},
                {"SUP-BGZG", "宝钢金属直供中心", "宝钢直供", "raw", "刘经理", "021-80000005", "liu@baosteel.com", "上海市宝山区富锦路1888号", "91310113MA1G0005XD", "中行上海宝山支行 6013820005088005", "预付款", "钢板及金属原材料供应商", 40, 1},
                {"SUP-NCXJ", "耐驰橡胶制品有限公司", "耐驰橡胶", "raw", "赵经理", "0512-80000004", "zhao@nachi.com", "无锡市新吴区长江路66号", "91320200MA1N0004XE", "农行无锡新吴支行 6228480004088004", "月结30天", "橡胶密封与减震件供应商", 50, 1},
                {"SUP-WLYS", "鸿运物流运输有限公司", "鸿运物流", "logistics", "孙主管", "0571-80000006", "sun@hongyun.com", "杭州市萧山区机场路9号", "91330109MA2K0006XF", "工行杭州萧山支行 6222021006088006", "月结30天", "采购运输与第三方物流服务商", 60, 1},
                {"SUP-WWFW", "维稳设备服务有限公司", "维稳服务", "service", "周工", "0571-80000007", "zhou@weiwen.com", "杭州市余杭区文一西路1500号", "91330110MA2L0007XG", "招行杭州余杭支行 6225880007088007", "季度结算", "设备维保与技术服务商", 70, 0}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO wms_supplier
                    (code, name, short_name, category, contact, phone, email, address, tax_no, bank_account, settle_type, description, sort, status, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10], row[11], row[12], row[13], LocalDateTime.now(), LocalDateTime.now());
        }
    }
}
