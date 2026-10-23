package com.qws.ets;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 电测模块建表与种子数据初始化：启动时创建 ets_register / ets_test / ets_report 三张表，
 * 幂等写入示例数据，便于前端页面直接展示。种子时间全部使用固定字面量，不使用 now()。
 */
@Component
public class EtsDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public EtsDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedRegister();
        seedTest();
        seedReport();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ets_register (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    register_no VARCHAR(50) NOT NULL,
                    customer_name VARCHAR(160),
                    contact VARCHAR(50),
                    phone VARCHAR(50),
                    product VARCHAR(160),
                    model VARCHAR(120),
                    brand VARCHAR(80),
                    batch_no VARCHAR(80),
                    quantity INT DEFAULT 0,
                    sample_qty INT DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'pending',
                    arrive_date DATE,
                    expect_date DATE,
                    test_standard VARCHAR(200),
                    operator VARCHAR(50),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_register_no (register_no),
                    INDEX idx_register_status (status),
                    INDEX idx_register_customer (customer_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ets_test (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL,
                    register_id BIGINT,
                    register_no VARCHAR(50),
                    customer_name VARCHAR(160),
                    product VARCHAR(160),
                    model VARCHAR(120),
                    test_item VARCHAR(160),
                    category VARCHAR(40),
                    test_standard VARCHAR(200),
                    standard_value VARCHAR(120),
                    measured_value VARCHAR(120),
                    result VARCHAR(20) NOT NULL DEFAULT 'pending',
                    tester VARCHAR(50),
                    test_time DATETIME,
                    duration_min DECIMAL(10,2),
                    equipment_used VARCHAR(160),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_test_code (code),
                    INDEX idx_test_register (register_id),
                    INDEX idx_test_result (result),
                    INDEX idx_test_category (category)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ets_report (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    report_no VARCHAR(50),
                    register_id BIGINT,
                    register_no VARCHAR(50),
                    customer_name VARCHAR(160),
                    product VARCHAR(160),
                    model VARCHAR(120),
                    batch_no VARCHAR(80),
                    quantity INT DEFAULT 0,
                    sample_qty INT DEFAULT 0,
                    pass_qty INT DEFAULT 0,
                    fail_qty INT DEFAULT 0,
                    pass_rate DECIMAL(6,2) DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'draft',
                    conclusion VARCHAR(500),
                    inspector VARCHAR(50),
                    issue_date DATETIME,
                    approver VARCHAR(50),
                    approve_time DATETIME,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_report_no (report_no),
                    INDEX idx_report_register (register_id),
                    INDEX idx_report_status (status),
                    INDEX idx_report_customer (customer_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedRegister() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ets_register", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        // {登记号, 客户, 联系人, 电话, 产品, 型号, 品牌, 批次, 数量, 抽样, 状态, 到货日, 预计完成日, 标准, 登记人, 备注, 创建时间}
        Object[][] seeds = new Object[][]{
                {"ETS20261001001", "北京科技有限公司", "张三", "13800138001", "低压断路器", "DZ47-63", "启维", "B20261001", 500, 20, "done",
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "GB/T 14048.2-2020", "李工", "常规出厂抽检", LocalDateTime.of(2026, 10, 1, 9, 20)},
                {"ETS20261003001", "上海贸易有限公司", "李四", "13800138002", "交流接触器", "CJX2-2510", "启维", "B20261003", 300, 15, "done",
                        LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7), "GB/T 14048.4-2020", "李工", "客户指定抽样", LocalDateTime.of(2026, 10, 3, 10, 5)},
                {"ETS20261008001", "广州电子公司", "王五", "13800138003", "漏电保护器", "DZ47LE-32", "安驰", "B20261008", 260, 13, "returned",
                        LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12), "GB/T 16917.1-2014", "周工", "抽样不合格，已退回", LocalDateTime.of(2026, 10, 8, 9, 0)},
                {"ETS20261012001", "深圳半导体公司", "赵六", "13800138004", "开关电源模块", "SPS-24V100W", "启维", "B20261012", 180, 12, "testing",
                        LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18), "GB 4943.1-2022", "周工", "耐压与温升加测", LocalDateTime.of(2026, 10, 12, 8, 40)},
                {"ETS20261015001", "武汉科技公司", "钱七", "13800138005", "控制变压器", "BK-500VA", "安驰", "B20261015", 240, 12, "received",
                        LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20), "GB/T 19212.1-2016", "孙工", "已收货待排期", LocalDateTime.of(2026, 10, 15, 11, 15)},
                {"ETS20261017001", "杭州自动化公司", "孙八", "13800138006", "电磁继电器", "JQX-13F", "启维", "B20261017", 600, 24, "testing",
                        LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 22), "GB/T 21711.1-2008", "孙工", "寿命测试中", LocalDateTime.of(2026, 10, 17, 9, 30)},
                {"ETS20261019001", "成都仪器公司", "周九", "13800138007", "电流互感器", "LMZ-0.5", "安驰", "B20261019", 320, 16, "pending",
                        LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 24), "GB 20840.2-2014", "李工", "待客户确认抽样方案", LocalDateTime.of(2026, 10, 19, 14, 0)},
                {"ETS20261020001", "苏州精密公司", "吴十", "13800138008", "熔断器底座", "RT18-32", "启维", "B20261020", 450, 18, "done",
                        LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23), "GB/T 13539.1-2015", "周工", "已出报告", LocalDateTime.of(2026, 10, 20, 9, 10)},
                {"ETS20261022001", "南京电气公司", "郑一", "13800138009", "时间继电器", "ST3PA-B", "安驰", "B20261022", 210, 10, "received",
                        LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 26), "GB/T 21711.1-2008", "孙工", "已收货", LocalDateTime.of(2026, 10, 22, 10, 20)},
                {"ETS20261023001", "天津机电公司", "王二", "13800138010", "配电箱", "XL-21", "启维", "B20261023", 90, 9, "pending",
                        LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 28), "GB/T 7251.1-2023", "李工", "整机耐压待排", LocalDateTime.of(2026, 10, 23, 8, 50)}
        };
        for (Object[] row : seeds) {
            jdbcTemplate.update("""
                    INSERT INTO ets_register
                    (register_no, customer_name, contact, phone, product, model, brand, batch_no,
                     quantity, sample_qty, status, arrive_date, expect_date, test_standard, operator,
                     remark, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7],
                    row[8], row[9], row[10], row[11], row[12], row[13], row[14],
                    row[15], row[16], row[16]);
        }
    }

    private void seedTest() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ets_test", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        // {编号, 登记号, 测试项目, 类别, 标准要求值, 实测值, 结果, 测试员, 测试时间, 耗时(分钟), 设备, 备注, 建立时间}
        Object[][] seeds = new Object[][]{
                {"T20261017001", "ETS20261001001", "外观与标志检查", "appearance", "标志清晰无破损", "符合要求", "pass", "李工", LocalDateTime.of(2026, 10, 17, 9, 30), "25.00", "目视检验台", "外观合格", LocalDateTime.of(2026, 10, 17, 9, 0)},
                {"T20261017002", "ETS20261001001", "工频耐压试验", "withstand", "2500V/1min 无击穿", "2500V/1min 未击穿", "pass", "李工", LocalDateTime.of(2026, 10, 17, 10, 20), "45.00", "耐压测试仪 AN9630", "合格", LocalDateTime.of(2026, 10, 17, 9, 50)},
                {"T20261017003", "ETS20261001001", "绝缘电阻测量", "insulation", ">=5MΩ", "120MΩ", "pass", "周工", LocalDateTime.of(2026, 10, 17, 11, 10), "20.00", "绝缘电阻表 ZC-7", "合格", LocalDateTime.of(2026, 10, 17, 10, 50)},
                {"T20261018001", "ETS20261003001", "外观与标志检查", "appearance", "标志清晰无破损", "外壳有轻微划痕", "pass", "周工", LocalDateTime.of(2026, 10, 18, 9, 0), "30.00", "目视检验台", "轻微划痕不影响判定", LocalDateTime.of(2026, 10, 18, 8, 40)},
                {"T20261018002", "ETS20261003001", "动作特性试验", "performance", "吸合电压<=85%Us", "吸合电压 78%Us", "pass", "周工", LocalDateTime.of(2026, 10, 18, 10, 30), "55.00", "接触器测试台", "合格", LocalDateTime.of(2026, 10, 18, 9, 50)},
                {"T20261019001", "ETS20261008001", "剩余动作电流测试", "performance", "IΔn<=30mA 应动作", "42mA 未动作", "fail", "孙工", LocalDateTime.of(2026, 10, 19, 9, 40), "50.00", "漏电保护测试仪", "动作电流超差", LocalDateTime.of(2026, 10, 19, 9, 0)},
                {"T20261019002", "ETS20261008001", "工频耐压试验", "withstand", "2500V/1min 无击穿", "1800V 击穿", "fail", "孙工", LocalDateTime.of(2026, 10, 19, 10, 50), "40.00", "耐压测试仪 AN9630", "绝缘失效", LocalDateTime.of(2026, 10, 19, 10, 10)},
                {"T20261020001", "ETS20261012001", "温升试验", "temperature", "温升<=60K", "温升 48K", "pass", "周工", LocalDateTime.of(2026, 10, 20, 9, 20), "120.00", "温升测试系统", "合格", LocalDateTime.of(2026, 10, 20, 8, 0)},
                {"T20261020002", "ETS20261012001", "接地连续性测试", "grounding", "<=0.1Ω", "0.045Ω", "pass", "周工", LocalDateTime.of(2026, 10, 20, 11, 30), "15.00", "接地电阻测试仪", "合格", LocalDateTime.of(2026, 10, 20, 11, 0)},
                {"T20261020003", "ETS20261012001", "输出纹波测试", "performance", "纹波<=100mV", "纹波 156mV", "fail", "孙工", LocalDateTime.of(2026, 10, 20, 14, 10), "35.00", "示波器 DSOX3024", "纹波超标", LocalDateTime.of(2026, 10, 20, 13, 40)},
                {"T20261021001", "ETS20261017001", "绝缘电阻测量", "insulation", ">=100MΩ", "500MΩ", "pass", "孙工", LocalDateTime.of(2026, 10, 21, 9, 10), "18.00", "绝缘电阻表 ZC-7", "合格", LocalDateTime.of(2026, 10, 21, 8, 50)},
                {"T20261021002", "ETS20261017001", "电寿命试验", "performance", ">=10万次", "12.5 万次", "pass", "孙工", LocalDateTime.of(2026, 10, 21, 15, 40), "240.00", "继电器寿命试验台", "合格", LocalDateTime.of(2026, 10, 21, 11, 0)},
                {"T20261022001", "ETS20261020001", "外观与标志检查", "appearance", "标志清晰无破损", "符合要求", "pass", "周工", LocalDateTime.of(2026, 10, 22, 9, 0), "22.00", "目视检验台", "合格", LocalDateTime.of(2026, 10, 22, 8, 40)},
                {"T20261022002", "ETS20261020001", "工频耐压试验", "withstand", "2000V/1min 无击穿", "2000V/1min 未击穿", "pass", "周工", LocalDateTime.of(2026, 10, 22, 10, 10), "38.00", "耐压测试仪 AN9630", "合格", LocalDateTime.of(2026, 10, 22, 9, 40)},
                {"T20261022003", "ETS20261020001", "温升试验", "temperature", "温升<=55K", "温升 61K", "fail", "李工", LocalDateTime.of(2026, 10, 22, 13, 30), "110.00", "温升测试系统", "温升超差", LocalDateTime.of(2026, 10, 22, 11, 40)},
                {"T20261023001", "ETS20261023001", "接地连续性测试", "grounding", "<=0.1Ω", "0.062Ω", "pass", "李工", LocalDateTime.of(2026, 10, 23, 9, 15), "16.00", "接地电阻测试仪", "合格", LocalDateTime.of(2026, 10, 23, 9, 0)},
                {"T20261023002", "ETS20261023001", "门板机械操作试验", "performance", "操作 50 次无卡滞", null, "pending", "李工", null, null, "机械操作台", "待排期", LocalDateTime.of(2026, 10, 23, 16, 20)}
        };
        for (Object[] row : seeds) {
            RegisterRow register = requireRegister((String) row[1]);
            jdbcTemplate.update("""
                    INSERT INTO ets_test
                    (code, register_id, register_no, customer_name, product, model, test_item, category,
                     test_standard, standard_value, measured_value, result, tester, test_time, duration_min,
                     equipment_used, remark, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], register.id(), register.registerNo(),
                    register.customerName(), register.product(), register.model(),
                    row[2], row[3], register.testStandard(), row[4], row[5], row[6],
                    row[7], row[8], row[9] == null ? null : new BigDecimal((String) row[9]),
                    row[10], row[11], row[12], row[12]);
        }
    }

    private void seedReport() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ets_report", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        // {报告号, 登记号, 抽样数, 合格数, 不合格数, 通过率, 状态, 结论, 检验员, 签发日, 审批人, 审批时间, 备注, 建立时间}
        Object[][] seeds = new Object[][]{
                {"RPT20261005001", "ETS20261001001", 20, 20, 0, "100.00", "approved", "所检项目全部合格",
                        "李工", LocalDateTime.of(2026, 10, 5, 10, 0), "张主管", LocalDateTime.of(2026, 10, 5, 14, 20), "同意出具报告", LocalDateTime.of(2026, 10, 5, 9, 0)},
                {"RPT20261007001", "ETS20261003001", 15, 15, 0, "100.00", "approved", "所检项目全部合格",
                        "周工", LocalDateTime.of(2026, 10, 7, 11, 0), "张主管", LocalDateTime.of(2026, 10, 7, 15, 0), "同意出具报告", LocalDateTime.of(2026, 10, 7, 9, 30)},
                {"RPT20261012001", "ETS20261008001", 13, 3, 10, "23.08", "rejected", "抽样中 10 台动作电流超差，整批退回",
                        "孙工", null, "张主管", LocalDateTime.of(2026, 10, 12, 16, 40), "结论与实测不符，退回重写", LocalDateTime.of(2026, 10, 11, 9, 0)},
                {"RPT20261018001", "ETS20261012001", 12, 11, 1, "91.67", "pending", "除纹波项目外其余合格，待客户确认",
                        "周工", null, null, null, "等待技术负责人审批", LocalDateTime.of(2026, 10, 18, 10, 0)},
                {"RPT20261022001", "ETS20261017001", 24, 24, 0, "100.00", "pending", "寿命与绝缘性能均合格",
                        "孙工", null, null, null, "待审批", LocalDateTime.of(2026, 10, 22, 15, 0)},
                {"RPT20261023001", "ETS20261020001", 18, 16, 2, "88.89", "approved", "温升项目 2 台超差，其余合格",
                        "周工", LocalDateTime.of(2026, 10, 23, 9, 30), "张主管", LocalDateTime.of(2026, 10, 23, 13, 10), "同意出具报告", LocalDateTime.of(2026, 10, 23, 8, 40)},
                {"RPT20261023002", "ETS20261023001", 9, 0, 0, "0.00", "draft", "草稿：机械操作项目尚未完成",
                        "李工", null, null, null, "测试未完成，暂为草稿", LocalDateTime.of(2026, 10, 23, 16, 30)}
        };
        for (Object[] row : seeds) {
            RegisterRow register = requireRegister((String) row[1]);
            jdbcTemplate.update("""
                    INSERT INTO ets_report
                    (report_no, register_id, register_no, customer_name, product, model, batch_no,
                     quantity, sample_qty, pass_qty, fail_qty, pass_rate, status, conclusion, inspector,
                     issue_date, approver, approve_time, remark, create_time, update_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row[0], register.id(), register.registerNo(),
                    register.customerName(), register.product(), register.model(),
                    register.batchNo(), register.quantity(), row[2], row[3], row[4],
                    new BigDecimal((String) row[5]), row[6], row[7], row[8], row[9],
                    row[10], row[11], row[12], row[13], row[13]);
        }
    }

    /** 按登记号查询来货登记，供测试/报告种子数据关联使用 */
    private RegisterRow requireRegister(String registerNo) {
        List<RegisterRow> rows = jdbcTemplate.query("""
                        SELECT id, register_no, customer_name, product, model, batch_no,
                               quantity, test_standard
                        FROM ets_register WHERE register_no = ? LIMIT 1
                        """,
                (rs, rowNum) -> new RegisterRow(
                        rs.getLong("id"),
                        rs.getString("register_no"),
                        rs.getString("customer_name"),
                        rs.getString("product"),
                        rs.getString("model"),
                        rs.getString("batch_no"),
                        rs.getObject("quantity") == null ? null : rs.getInt("quantity"),
                        rs.getString("test_standard")),
                registerNo);
        if (rows.isEmpty()) {
            throw new IllegalStateException("种子数据缺失：来货登记 " + registerNo);
        }
        return rows.get(0);
    }

    /** 来货登记种子关联字段 */
    private record RegisterRow(Long id, String registerNo, String customerName, String product,
                               String model, String batchNo, Integer quantity, String testStandard) {
    }
}
