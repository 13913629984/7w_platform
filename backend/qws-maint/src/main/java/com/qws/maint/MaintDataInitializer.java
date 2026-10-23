package com.qws.maint;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 维保子系统建表与种子数据初始化。
 * 采用 CREATE TABLE IF NOT EXISTS + COUNT 幂等判断，参考 WMS/CRM 模块的初始化方式。
 */
@Component
public class MaintDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public MaintDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createEquipmentTable();
        createWorkOrderTable();
        createPlanTable();
        createSparePartTable();
        seedEquipments();
        seedWorkOrders();
        seedPlans();
        seedSpareParts();
    }

    private void createEquipmentTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS maint_equipment (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    model VARCHAR(100),
                    brand VARCHAR(100),
                    category VARCHAR(50),
                    customer_id BIGINT,
                    customer_name VARCHAR(100),
                    location VARCHAR(200),
                    install_date DATE,
                    warranty_end DATE,
                    status VARCHAR(20) NOT NULL DEFAULT 'running',
                    manager VARCHAR(50),
                    phone VARCHAR(50),
                    purchase_price DECIMAL(14,2),
                    last_maintain_date DATE,
                    next_maintain_date DATE,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_equipment_status (status),
                    INDEX idx_equipment_customer (customer_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void createWorkOrderTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS maint_work_order (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    title VARCHAR(200) NOT NULL,
                    equipment_id BIGINT,
                    equipment_code VARCHAR(50),
                    equipment_name VARCHAR(100),
                    customer_name VARCHAR(100),
                    type VARCHAR(20) NOT NULL DEFAULT 'repair',
                    priority VARCHAR(20) NOT NULL DEFAULT 'normal',
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    reporter VARCHAR(50),
                    handler VARCHAR(50),
                    report_time DATETIME,
                    plan_time DATETIME,
                    finish_time DATETIME,
                    hours DECIMAL(8,2),
                    cost DECIMAL(14,2),
                    fault_desc VARCHAR(1000),
                    solution VARCHAR(1000),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_work_order_status (status),
                    INDEX idx_work_order_priority (priority),
                    INDEX idx_work_order_equipment (equipment_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void createPlanTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS maint_plan (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    equipment_id BIGINT,
                    equipment_code VARCHAR(50),
                    equipment_name VARCHAR(100),
                    cycle_type VARCHAR(20) NOT NULL DEFAULT 'month',
                    cycle_value INT NOT NULL DEFAULT 1,
                    start_date DATE,
                    next_date DATE,
                    items VARCHAR(500),
                    manager VARCHAR(50),
                    status VARCHAR(20) NOT NULL DEFAULT 'active',
                    remark VARCHAR(500),
                    last_execute_date DATE,
                    execute_count INT NOT NULL DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_plan_status (status),
                    INDEX idx_plan_next_date (next_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void createSparePartTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS maint_spare_part (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    spec VARCHAR(200),
                    unit VARCHAR(20),
                    category VARCHAR(50),
                    warehouse_code VARCHAR(50),
                    stock_qty DECIMAL(14,2) NOT NULL DEFAULT 0,
                    safety_qty DECIMAL(14,2) NOT NULL DEFAULT 0,
                    price DECIMAL(14,2),
                    supplier VARCHAR(100),
                    status TINYINT NOT NULL DEFAULT 1,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_spare_part_category (category),
                    INDEX idx_spare_part_warehouse (warehouse_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private boolean isEmpty(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null || count == 0;
    }

    private void seedEquipments() {
        if (!isEmpty("maint_equipment")) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"EQ-2026-001", "一号线主控柜", "GCK-800", "七维", "控制柜", 1L, "杭州精工机械有限公司", "1 号车间东侧",
                        LocalDate.of(2024, 3, 12), LocalDate.of(2027, 3, 11), "running", "张伟", "13800010001",
                        new BigDecimal("128000.00"), LocalDate.of(2026, 9, 18), LocalDate.of(2026, 11, 18)},
                {"EQ-2026-002", "S7-1500 主控 PLC", "6ES7515-2AM02", "西门子", "PLC", 1L, "杭州精工机械有限公司", "1 号车间电控室",
                        LocalDate.of(2024, 3, 15), LocalDate.of(2027, 3, 14), "running", "李强", "13800010002",
                        new BigDecimal("46000.00"), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 12)},
                {"EQ-2026-003", "ABB 变频器 45kW", "ACS580-01-106A", "ABB", "变频器", 2L, "宁波海天塑机集团", "注塑车间 3 区",
                        LocalDate.of(2023, 9, 1), LocalDate.of(2026, 8, 31), "repair", "王敏", "13800010003",
                        new BigDecimal("32500.00"), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 20)},
                {"EQ-2026-004", "安川伺服电机 2kW", "SGM7J-2KW", "安川", "伺服电机", 2L, "宁波海天塑机集团", "注塑车间 1 区",
                        LocalDate.of(2023, 9, 5), LocalDate.of(2026, 9, 4), "running", "赵磊", "13800010004",
                        new BigDecimal("18600.00"), LocalDate.of(2026, 9, 28), LocalDate.of(2026, 12, 28)},
                {"EQ-2026-005", "自动化装配线控制柜", "QWS-AC-2200", "七维", "控制柜", 3L, "苏州联达精密制造有限公司", "装配车间 A 区",
                        LocalDate.of(2025, 1, 20), LocalDate.of(2028, 1, 19), "idle", "陈刚", "13800010005",
                        new BigDecimal("156000.00"), LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 15)},
                {"EQ-2026-006", "欧姆龙温控模块", "CJ1W-TC004", "欧姆龙", "PLC", 3L, "苏州联达精密制造有限公司", "装配车间电控柜",
                        LocalDate.of(2025, 1, 22), LocalDate.of(2028, 1, 21), "running", "周涛", "13800010006",
                        new BigDecimal("7200.00"), LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 10)},
                {"EQ-2026-007", "施耐德软启动器 90kW", "ATS480-90", "施耐德", "变频器", 4L, "绍兴越新纺织股份有限公司", "纺纱车间动力房",
                        LocalDate.of(2022, 11, 8), LocalDate.of(2026, 11, 30), "repair", "张伟", "13800010007",
                        new BigDecimal("29800.00"), LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 25)},
                {"EQ-2026-008", "旧式继电器控制柜", "GCK-600", "七维", "控制柜", 4L, "绍兴越新纺织股份有限公司", "织造车间西侧",
                        LocalDate.of(2018, 7, 16), LocalDate.of(2021, 7, 15), "scrapped", "李强", "13800010008",
                        new BigDecimal("38000.00"), LocalDate.of(2026, 6, 20), LocalDate.of(2026, 9, 20)}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO maint_equipment
                    (code, name, model, brand, category, customer_id, customer_name, location, install_date,
                     warranty_end, status, manager, phone, purchase_price, last_maintain_date, next_maintain_date,
                     remark, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10],
                    row[11], row[12], row[13], row[14], row[15], "种子数据", LocalDateTime.of(2026, 9, 5, 9, 0),
                    LocalDateTime.of(2026, 10, 20, 15, 30));
        }
    }

    private void seedWorkOrders() {
        if (!isEmpty("maint_work_order")) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"WO20261012001", "主控柜接触器异响排查", 1L, "EQ-2026-001", "一号线主控柜", "杭州精工机械有限公司",
                        "repair", "high", "closed", "刘洋", "张伟",
                        LocalDateTime.of(2026, 10, 12, 8, 30), LocalDateTime.of(2026, 10, 12, 14, 0),
                        LocalDateTime.of(2026, 10, 12, 16, 20), new BigDecimal("3.50"), new BigDecimal("480.00"),
                        "接触器吸合时发出明显异响", "更换交流接触器并紧固接线端子"},
                {"WO20261014002", "PLC 程序通讯超时处理", 2L, "EQ-2026-002", "S7-1500 主控 PLC", "杭州精工机械有限公司",
                        "repair", "urgent", "closed", "刘洋", "李强",
                        LocalDateTime.of(2026, 10, 14, 9, 15), LocalDateTime.of(2026, 10, 14, 11, 0),
                        LocalDateTime.of(2026, 10, 14, 12, 40), new BigDecimal("2.00"), new BigDecimal("650.00"),
                        "上位机与 PLC 通讯间断中断", "更换交换机网口并重做水晶头"},
                {"WO20261015003", "变频器过流报警处理", 3L, "EQ-2026-003", "ABB 变频器 45kW", "宁波海天塑机集团",
                        "repair", "urgent", "completed", "赵强", "王敏",
                        LocalDateTime.of(2026, 10, 15, 10, 20), LocalDateTime.of(2026, 10, 16, 9, 0),
                        LocalDateTime.of(2026, 10, 16, 15, 10), new BigDecimal("5.00"), new BigDecimal("1280.00"),
                        "运行中频繁报过流故障代码", "清理散热风道并更换散热风扇"},
                {"WO20261016004", "伺服电机编码器线更换", 4L, "EQ-2026-004", "安川伺服电机 2kW", "宁波海天塑机集团",
                        "repair", "normal", "completed", "赵强", "赵磊",
                        LocalDateTime.of(2026, 10, 16, 13, 40), LocalDateTime.of(2026, 10, 17, 9, 0),
                        LocalDateTime.of(2026, 10, 17, 11, 30), new BigDecimal("2.50"), new BigDecimal("760.00"),
                        "编码器信号抖动导致定位偏差", "更换编码器线束并重新标定零点"},
                {"WO20261017005", "装配线控制柜季度保养", 5L, "EQ-2026-005", "自动化装配线控制柜", "苏州联达精密制造有限公司",
                        "preventive", "low", "closed", "系统计划", "陈刚",
                        LocalDateTime.of(2026, 10, 17, 8, 0), LocalDateTime.of(2026, 10, 17, 10, 0),
                        LocalDateTime.of(2026, 10, 17, 12, 15), new BigDecimal("4.00"), new BigDecimal("300.00"),
                        "季度例行保养", "柜内除尘、紧固端子、测试绝缘"},
                {"WO20261018006", "温控模块测温漂移校验", 6L, "EQ-2026-006", "欧姆龙温控模块", "苏州联达精密制造有限公司",
                        "inspection", "normal", "completed", "系统计划", "周涛",
                        LocalDateTime.of(2026, 10, 18, 9, 0), LocalDateTime.of(2026, 10, 18, 14, 0),
                        LocalDateTime.of(2026, 10, 18, 15, 45), new BigDecimal("2.00"), new BigDecimal("260.00"),
                        "热电偶读数偏差约 5℃", "更换热电偶补偿导线并重新整定"},
                {"WO20261019007", "软启动器无法启动故障", 7L, "EQ-2026-007", "施耐德软启动器 90kW", "绍兴越新纺织股份有限公司",
                        "repair", "urgent", "processing", "沈鹏", "张伟",
                        LocalDateTime.of(2026, 10, 19, 7, 50), LocalDateTime.of(2026, 10, 19, 9, 0),
                        null, null, null,
                        "合闸后无输出，面板报 F001 故障", null},
                {"WO20261020008", "继电器控制柜改造评估", 8L, "EQ-2026-008", "旧式继电器控制柜", "绍兴越新纺织股份有限公司",
                        "install", "low", "assigned", "沈鹏", "李强",
                        LocalDateTime.of(2026, 10, 20, 10, 0), LocalDateTime.of(2026, 10, 26, 9, 0),
                        null, null, null,
                        "设备老化，评估整体改造或报废", null},
                {"WO20261020009", "主控柜年度预防性保养", 1L, "EQ-2026-001", "一号线主控柜", "杭州精工机械有限公司",
                        "preventive", "normal", "closed", "系统计划", "张伟",
                        LocalDateTime.of(2026, 10, 20, 8, 30), LocalDateTime.of(2026, 10, 20, 9, 0),
                        LocalDateTime.of(2026, 10, 20, 12, 0), new BigDecimal("3.50"), new BigDecimal("320.00"),
                        "年度预防性保养", "清洁柜内积尘、检查接地、紧固母排"},
                {"WO20261021010", "PLC 输入模块指示灯异常", 2L, "EQ-2026-002", "S7-1500 主控 PLC", "杭州精工机械有限公司",
                        "repair", "high", "processing", "刘洋", "李强",
                        LocalDateTime.of(2026, 10, 21, 9, 30), LocalDateTime.of(2026, 10, 21, 10, 30),
                        null, null, null,
                        "DI 模块第 8 点指示灯常亮无信号变化", null},
                {"WO20261022011", "注塑车间变频器巡检", 3L, "EQ-2026-003", "ABB 变频器 45kW", "宁波海天塑机集团",
                        "inspection", "normal", "assigned", "系统计划", "王敏",
                        LocalDateTime.of(2026, 10, 22, 14, 0), LocalDateTime.of(2026, 10, 28, 9, 0),
                        null, null, null,
                        "例行巡检振动与温度", null},
                {"WO20261023012", "备件更换申请：编码器", 4L, "EQ-2026-004", "安川伺服电机 2kW", "宁波海天塑机集团",
                        "repair", "normal", "pending", "赵强", null,
                        LocalDateTime.of(2026, 10, 23, 8, 10), LocalDateTime.of(2026, 10, 25, 9, 0),
                        null, null, null,
                        "编码器偶发丢脉冲，需更换备件", null}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO maint_work_order
                    (code, title, equipment_id, equipment_code, equipment_name, customer_name, type, priority,
                     status, reporter, handler, report_time, plan_time, finish_time, hours, cost, fault_desc,
                     solution, remark, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10],
                    row[11], row[12], row[13], row[14], row[15], row[16], row[17], "种子数据",
                    LocalDateTime.of(2026, 10, 12, 8, 35), LocalDateTime.of(2026, 10, 23, 9, 15));
        }
    }

    private void seedPlans() {
        if (!isEmpty("maint_plan")) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"MP20260001", "主控柜月度保养", 1L, "EQ-2026-001", "一号线主控柜", "month", 1,
                        LocalDate.of(2026, 5, 18), LocalDate.of(2026, 11, 18), "柜内除尘,端子紧固,绝缘测试", "张伟",
                        "active", LocalDate.of(2026, 10, 18), 5},
                {"MP20260002", "PLC 季度点检", 2L, "EQ-2026-002", "S7-1500 主控 PLC", "quarter", 1,
                        LocalDate.of(2026, 5, 12), LocalDate.of(2026, 11, 12), "程序备份,电池电压检测,通讯测试", "李强",
                        "active", LocalDate.of(2026, 8, 12), 2},
                {"MP20260003", "变频器半月除尘", 3L, "EQ-2026-003", "ABB 变频器 45kW", "day", 15,
                        LocalDate.of(2026, 9, 5), LocalDate.of(2026, 10, 20), "散热风道清理,风扇运行检查", "王敏",
                        "active", LocalDate.of(2026, 10, 5), 7},
                {"MP20260004", "伺服电机年度检修", 4L, "EQ-2026-004", "安川伺服电机 2kW", "year", 1,
                        LocalDate.of(2026, 9, 28), LocalDate.of(2027, 9, 28), "轴承更换,润滑加注,编码器标定", "赵磊",
                        "active", LocalDate.of(2026, 9, 28), 1},
                {"MP20260005", "装配线控制柜周保养", 5L, "EQ-2026-005", "自动化装配线控制柜", "week", 2,
                        LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 15), "柜面清洁,指示灯检查,风扇检查", "陈刚",
                        "paused", LocalDate.of(2026, 9, 30), 4},
                {"MP20260006", "温控模块月度校验", 6L, "EQ-2026-006", "欧姆龙温控模块", "month", 1,
                        LocalDate.of(2026, 5, 10), LocalDate.of(2026, 11, 10), "热电偶校验,温控参数核对", "周涛",
                        "finished", LocalDate.of(2026, 10, 10), 4}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO maint_plan
                    (code, name, equipment_id, equipment_code, equipment_name, cycle_type, cycle_value, start_date,
                     next_date, items, manager, status, last_execute_date, execute_count, remark, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10],
                    row[11], row[12], row[13], "种子数据", LocalDateTime.of(2026, 9, 10, 9, 30),
                    LocalDateTime.of(2026, 10, 18, 10, 0));
        }
    }

    private void seedSpareParts() {
        if (!isEmpty("maint_spare_part")) {
            return;
        }
        Object[][] seeds = new Object[][]{
                {"SP0001", "交流接触器", "CJX2-2510 25A", "个", "电气元件", "WH-MAINT-A",
                        new BigDecimal("12"), new BigDecimal("20"), new BigDecimal("86.00"), "正泰电气", 1},
                {"SP0002", "中间继电器", "MY2NJ DC24V", "个", "电气元件", "WH-MAINT-A",
                        new BigDecimal("35"), new BigDecimal("30"), new BigDecimal("22.50"), "欧姆龙", 1},
                {"SP0003", "伺服编码器线", "5m 屏蔽双绞", "根", "线缆", "WH-MAINT-B",
                        new BigDecimal("3"), new BigDecimal("6"), new BigDecimal("420.00"), "安川原厂", 1},
                {"SP0004", "变频器散热风扇", "AFB1224EHE 24V", "个", "散热件", "WH-MAINT-B",
                        new BigDecimal("8"), new BigDecimal("10"), new BigDecimal("135.00"), "台达电子", 1},
                {"SP0005", "PLC 输入模块", "SM521 DI 16x24VDC", "块", "电气元件", "WH-MAINT-A",
                        new BigDecimal("2"), new BigDecimal("4"), new BigDecimal("1580.00"), "西门子", 1},
                {"SP0006", "热电偶", "K 型 3m 铠装", "支", "仪表件", "WH-MAINT-C",
                        new BigDecimal("18"), new BigDecimal("15"), new BigDecimal("98.00"), "上自仪表", 1},
                {"SP0007", "工业交换机", "8 口百兆导轨式", "台", "网络设备", "WH-MAINT-C",
                        new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("860.00"), "摩莎科技", 1},
                {"SP0008", "断路器", "NXB-63 C32", "个", "电气元件", "WH-MAINT-A",
                        new BigDecimal("26"), new BigDecimal("20"), new BigDecimal("46.00"), "正泰电气", 1},
                {"SP0009", "密封胶条", "10mm EPDM", "米", "辅材", "WH-MAINT-C",
                        new BigDecimal("120"), new BigDecimal("100"), new BigDecimal("8.50"), "耐驰橡胶", 1},
                {"SP0010", "备用电源模块", "24V 5A 导轨式", "台", "电源", "WH-MAINT-B",
                        new BigDecimal("4"), new BigDecimal("8"), new BigDecimal("320.00"), "明纬电子", 1}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO maint_spare_part
                    (code, name, spec, unit, category, warehouse_code, stock_qty, safety_qty, price, supplier,
                     status, remark, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10],
                    "种子数据", LocalDateTime.of(2026, 9, 8, 10, 0), LocalDateTime.of(2026, 10, 20, 11, 20));
        }
    }
}
