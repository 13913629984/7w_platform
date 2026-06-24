package com.qws.fin;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 财务模块建表与种子数据初始化：在启动时创建 fin_* 表并写入示例数据，
 * 种子数据与前端财务页面原有的静态数据保持一致，保证页面展示效果不变。
 */
@Component
public class FinDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    public FinDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedReceivable();
        seedPayable();
        seedPayment();
        seedExpense();
        seedBudget();
        seedCost();
        seedRecord();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_receivable (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    customer VARCHAR(160),
                    sales_order VARCHAR(80),
                    contract VARCHAR(80),
                    amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    written_off DECIMAL(16,2) NOT NULL DEFAULT 0,
                    pending DECIMAL(16,2) NOT NULL DEFAULT 0,
                    invoiced TINYINT NOT NULL DEFAULT 0,
                    due_date VARCHAR(20),
                    status VARCHAR(30) NOT NULL DEFAULT '待核销',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_status (status), INDEX idx_customer (customer)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_receipt_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    receivable_id BIGINT,
                    receivable_code VARCHAR(80),
                    customer VARCHAR(160),
                    amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    method VARCHAR(40),
                    date VARCHAR(20),
                    operator VARCHAR(60),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_receivable (receivable_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_payable (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    receipt_code VARCHAR(80),
                    supplier VARCHAR(160),
                    purchase_order VARCHAR(80),
                    purchase_amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    tax DECIMAL(16,2) NOT NULL DEFAULT 0,
                    payable DECIMAL(16,2) NOT NULL DEFAULT 0,
                    paid DECIMAL(16,2) NOT NULL DEFAULT 0,
                    pending DECIMAL(16,2) NOT NULL DEFAULT 0,
                    matched TINYINT NOT NULL DEFAULT 0,
                    due_date VARCHAR(20),
                    status VARCHAR(30) NOT NULL DEFAULT '待匹配',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_status (status), INDEX idx_supplier (supplier)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_payment (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    type VARCHAR(40),
                    ap_code VARCHAR(80),
                    payee VARCHAR(160),
                    amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    method VARCHAR(40),
                    apply_date VARCHAR(40),
                    status VARCHAR(30) NOT NULL DEFAULT '待审批',
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_expense (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    type VARCHAR(40),
                    module VARCHAR(40),
                    item VARCHAR(160),
                    amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    date VARCHAR(20),
                    applicant VARCHAR(60),
                    approved TINYINT NOT NULL DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_type (type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_budget (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    dept VARCHAR(60),
                    item VARCHAR(120),
                    budget DECIMAL(16,2) NOT NULL DEFAULT 0,
                    executed DECIMAL(16,2) NOT NULL DEFAULT 0,
                    rate DECIMAL(8,2) NOT NULL DEFAULT 0,
                    year INT,
                    month INT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_dept (dept)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_cost_collection (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    cost_type VARCHAR(40),
                    source VARCHAR(160),
                    amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    date VARCHAR(40),
                    status VARCHAR(30) NOT NULL DEFAULT '待处理',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_cost_type (cost_type)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_cost_allocation (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    collection_id BIGINT,
                    collection_code VARCHAR(80),
                    cost_type VARCHAR(40),
                    object VARCHAR(160),
                    amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    ratio VARCHAR(20),
                    date VARCHAR(40),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_collection (collection_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fin_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    biz_type VARCHAR(40),
                    module VARCHAR(60),
                    amount DECIMAL(16,2) NOT NULL DEFAULT 0,
                    direction VARCHAR(20),
                    status VARCHAR(30),
                    date VARCHAR(20),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_direction (direction)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private boolean empty(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null || count == 0;
    }

    private void seedReceivable() {
        if (empty("fin_receivable")) {
            String sql = "INSERT INTO fin_receivable (code, customer, sales_order, contract, amount, written_off, pending, invoiced, due_date, status) VALUES (?,?,?,?,?,?,?,?,?,?)";
            jdbcTemplate.update(sql, "AR20260528001", "华为技术有限公司", "SO20260528001", "HT-2026-0528", 5800000, 4060000, 1740000, 1, "2026-06-28", "部分核销");
            jdbcTemplate.update(sql, "AR20260527001", "比亚迪股份有限公司", "SO20260527001", "HT-2026-0527", 8600000, 0, 8600000, 1, "2026-06-27", "待核销");
            jdbcTemplate.update(sql, "AR20260526001", "宁德时代新能源", "SO20260526001", "HT-2026-0526", 12000000, 6000000, 6000000, 0, "2026-06-26", "部分核销");
            jdbcTemplate.update(sql, "AR20260525001", "小米科技有限公司", "SO20260525001", "HT-2026-0525", 3200000, 3200000, 0, 1, "2026-05-25", "已核销");
            jdbcTemplate.update(sql, "AR20260524001", "大疆创新科技有限公司", "SO20260524001", "HT-2026-0524", 1500000, 750000, 750000, 1, "2026-05-10", "逾期");
        }
        if (empty("fin_receipt_record")) {
            String sql = "INSERT INTO fin_receipt_record (code, receivable_code, customer, amount, method, date, operator) VALUES (?,?,?,?,?,?,?)";
            jdbcTemplate.update(sql, "RK20260528001", "AR20260528001", "华为技术有限公司", 4060000, "银行转账", "2026-06-15", "张三");
            jdbcTemplate.update(sql, "RK20260526001", "AR20260526001", "宁德时代新能源", 6000000, "银行转账", "2026-06-12", "李四");
            jdbcTemplate.update(sql, "RK20260525001", "AR20260525001", "小米科技有限公司", 3200000, "承兑汇票", "2026-05-20", "王五");
            jdbcTemplate.update(sql, "RK20260524001", "AR20260524001", "大疆创新科技有限公司", 750000, "银行转账", "2026-05-08", "张三");
        }
    }

    private void seedPayable() {
        if (!empty("fin_payable")) return;
        String sql = "INSERT INTO fin_payable (code, receipt_code, supplier, purchase_order, purchase_amount, tax, payable, paid, pending, matched, due_date, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, "AP20260528001", "RK20260527001", "上海芯片公司", "PO20260527001", 8500, 1105, 9605, 0, 9605, 1, "2026-06-10", "待付款");
        jdbcTemplate.update(sql, "AP20260527001", "RK20260526001", "苏州原材料厂", "PO20260526001", 6500, 845, 7345, 0, 7345, 0, "2026-06-05", "待匹配");
        jdbcTemplate.update(sql, "AP20260526001", "RK20260525001", "宁波塑胶有限公司", "PO20260525001", 35200, 4576, 39776, 39776, 0, 1, "2026-05-28", "已付款");
        jdbcTemplate.update(sql, "AP20260525001", "RK20260524001", "深圳电子科技", "PO20260524001", 45600, 5928, 51528, 0, 51528, 0, "2026-06-02", "逾期");
    }

    private void seedPayment() {
        if (!empty("fin_payment")) return;
        String sql = "INSERT INTO fin_payment (code, type, ap_code, payee, amount, method, apply_date, status) VALUES (?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, "PAY20260526001", "采购付款", "AP20260526001", "宁波CC塑胶", 39776, "银行转账", "2026-05-28 10:30:00", "已付款");
        jdbcTemplate.update(sql, "PAY20260528002", "采购付款", "AP20260528001", "上海芯片公司", 9605, "银行转账", "2026-05-28 09:15:00", "已通过");
        jdbcTemplate.update(sql, "PAY20260527001", "费用报销", "", "张销售", 3500, "支付宝", "2026-05-27 14:00:00", "待审批");
        jdbcTemplate.update(sql, "PAY20260526002", "采购付款", "AP20260525001", "深圳电子科技", 51528, "银行转账", "2026-05-26 11:00:00", "审批中");
        jdbcTemplate.update(sql, "PAY20260525001", "费用报销", "", "王研发", 12800, "银行转账", "2026-05-25 16:00:00", "已驳回");
    }

    private void seedExpense() {
        if (!empty("fin_expense")) return;
        String sql = "INSERT INTO fin_expense (code, type, module, item, amount, date, applicant, approved) VALUES (?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, "EXP-2026-0528-001", "销售费用", "CRM", "客户招待费", 3500, "2026-05-28", "张销售", 1);
        jdbcTemplate.update(sql, "EXP-2026-0527-002", "管理费用", "系统", "办公设备采购", 8200, "2026-05-27", "李行政", 0);
        jdbcTemplate.update(sql, "EXP-2026-0526-003", "研发费用", "系统", "云服务器费用", 12800, "2026-05-26", "王研发", 1);
        jdbcTemplate.update(sql, "EXP-2026-0525-004", "财务费用", "系统", "银行手续费", 560, "2026-05-25", "财务部", 1);
        jdbcTemplate.update(sql, "EXP-2026-0524-005", "销售费用", "CRM", "差旅费", 4200, "2026-05-24", "赵销售", 1);
        jdbcTemplate.update(sql, "EXP-2026-0523-006", "管理费用", "维保", "车辆维修", 2350, "2026-05-23", "孙车队", 0);
    }

    private void seedBudget() {
        if (!empty("fin_budget")) return;
        String sql = "INSERT INTO fin_budget (dept, item, budget, executed, rate, year, month) VALUES (?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, "销售部", "销售费用预算", 120, 58.6, 48.8, 2026, 5);
        jdbcTemplate.update(sql, "研发部", "研发费用预算", 380, 168.5, 44.3, 2026, 5);
        jdbcTemplate.update(sql, "生产部", "生产成本预算", 1200, 856.2, 71.4, 2026, 5);
        jdbcTemplate.update(sql, "管理部", "管理费用预算", 280, 198.6, 70.9, 2026, 5);
        jdbcTemplate.update(sql, "采购部", "采购成本预算", 700, 320.5, 45.8, 2026, 5);
    }

    private void seedCost() {
        if (empty("fin_cost_collection")) {
            String sql = "INSERT INTO fin_cost_collection (code, cost_type, source, amount, date, status) VALUES (?,?,?,?,?,?)";
            jdbcTemplate.update(sql, "CC20260528001", "原材料", "采购入库归集", 120000, "2026-05-28", "已分配");
            jdbcTemplate.update(sql, "CC20260527001", "设备采购", "设备采购归集", 68000, "2026-05-27", "已分配");
            jdbcTemplate.update(sql, "CC20260526001", "运输费", "物流运输归集", 18000, "2026-05-26", "待处理");
            jdbcTemplate.update(sql, "CC20260525001", "制造费用", "生产制造归集", 8600, "2026-05-25", "已分配");
        }
        if (empty("fin_cost_allocation")) {
            String sql = "INSERT INTO fin_cost_allocation (code, collection_code, cost_type, object, amount, ratio, date) VALUES (?,?,?,?,?,?,?)";
            jdbcTemplate.update(sql, "FP20260528001", "CC20260528001", "原材料", "A产品线", 72000, "60%", "2026-05-28 10:00:00");
            jdbcTemplate.update(sql, "FP20260528002", "CC20260528001", "原材料", "B产品线", 48000, "40%", "2026-05-28 10:05:00");
            jdbcTemplate.update(sql, "FP20260527001", "CC20260527001", "设备采购", "生产部", 68000, "100%", "2026-05-27 14:00:00");
            jdbcTemplate.update(sql, "FP20260525001", "CC20260525001", "制造费用", "A产品线", 8600, "100%", "2026-05-25 09:00:00");
        }
    }

    private void seedRecord() {
        if (!empty("fin_record")) return;
        String sql = "INSERT INTO fin_record (code, biz_type, module, amount, direction, status, date) VALUES (?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, "FIN-2026-0528-001", "销售回款", "CRM 合同", 128000, "收入", "已确认", "2026-05-28");
        jdbcTemplate.update(sql, "FIN-2026-0528-002", "采购付款", "采购订单", 45600, "支出", "已支付", "2026-05-28");
        jdbcTemplate.update(sql, "FIN-2026-0527-003", "维保结算", "维保工单", 8500, "支出", "处理中", "2026-05-27");
        jdbcTemplate.update(sql, "FIN-2026-0527-004", "销售回款", "CRM 合同", 96000, "收入", "已确认", "2026-05-27");
        jdbcTemplate.update(sql, "FIN-2026-0526-005", "费用报销", "费用管理", 3500, "支出", "已支付", "2026-05-26");
        jdbcTemplate.update(sql, "FIN-2026-0526-006", "电测收入", "电测系统", 32000, "收入", "已确认", "2026-05-26");
    }
}
