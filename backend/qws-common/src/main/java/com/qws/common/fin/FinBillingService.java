package com.qws.common.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinPayable;
import com.qws.common.entity.FinReceivable;
import com.qws.common.entity.FinRecord;
import com.qws.common.mapper.FinPayableMapper;
import com.qws.common.mapper.FinReceivableMapper;
import com.qws.common.mapper.FinRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 跨模块财务单据生成（CRM/WMS → FIN 的缝合点）。
 *
 * <p>背景：平台采用「单库多服务」部署，所有服务共用 {@code 7wsys} 库，因此业务服务可以直接
 * 写 {@code fin_*} 表，而不必引入服务间 HTTP 调用（那会带来分布式事务问题）。
 * 本类集中承担「业务事件 → 财务单据」的映射，避免各服务各写一套。
 *
 * <p>两条主链路：
 * <ul>
 *   <li>销售出库完成 → 应收款（AR）+ 收入流水；</li>
 *   <li>采购入库完成 → 应付款（AP）+ 支出流水。</li>
 * </ul>
 *
 * <p>所有方法都是<b>幂等</b>的：按业务单号查重，重复调用只返回已有单据号，
 * 保证出库/入库被反复执行（例如部分出库分多次完成）时不会产生重复财务单据。
 */
@Service
public class FinBillingService {

    private static final Logger log = LoggerFactory.getLogger(FinBillingService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FinReceivableMapper receivableMapper;
    private final FinPayableMapper payableMapper;
    private final FinRecordMapper recordMapper;

    public FinBillingService(FinReceivableMapper receivableMapper,
                             FinPayableMapper payableMapper,
                             FinRecordMapper recordMapper) {
        this.receivableMapper = receivableMapper;
        this.payableMapper = payableMapper;
        this.recordMapper = recordMapper;
    }

    /**
     * 销售出库完成后生成应收款，同时写一条收入流水。
     *
     * @param salesOrder 销售订单号（幂等键，同时用于对账）
     * @param contract   合同号
     * @param customer   客户名称
     * @param amount     应收金额（元）
     * @param dueDays    账期天数（按出库日 + N 天算到期日）
     * @return 应收单号；参数不完整或金额非正时返回 null（业务上不算错误，跳过即可）
     */
    @Transactional
    public String createReceivableForSales(String salesOrder, String contract, String customer,
                                           BigDecimal amount, int dueDays) {
        if (!StringUtils.hasText(customer) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("跳过应收生成：客户或金额不合法 salesOrder={} customer={} amount={}", salesOrder, customer, amount);
            return null;
        }
        if (StringUtils.hasText(salesOrder)) {
            FinReceivable existing = receivableMapper.selectOne(new LambdaQueryWrapper<FinReceivable>()
                    .eq(FinReceivable::getSalesOrder, salesOrder.trim()).last("LIMIT 1"));
            if (existing != null) {
                log.debug("应收已存在，跳过 salesOrder={} code={}", salesOrder, existing.getCode());
                return existing.getCode();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate due = LocalDate.now().plusDays(Math.max(dueDays, 0));
        FinReceivable entity = new FinReceivable();
        entity.setCode(dailyCode("AR", FinReceivable.class.getSimpleName()));
        entity.setCustomer(customer.trim());
        entity.setSalesOrder(StringUtils.hasText(salesOrder) ? salesOrder.trim() : "");
        entity.setContract(StringUtils.hasText(contract) ? contract.trim() : "");
        entity.setAmount(amount);
        entity.setWrittenOff(BigDecimal.ZERO);
        entity.setPending(amount);
        entity.setInvoiced(0);
        entity.setDueDate(due.toString());
        entity.setStatus("待核销");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        receivableMapper.insert(entity);

        writeRecord("销售收入", "CRM 合同", amount, "收入", "已完成", now);
        log.info("销售出库生成应收 salesOrder={} code={} amount={}", salesOrder, entity.getCode(), amount);
        return entity.getCode();
    }

    /**
     * 采购入库完成后生成应付款，同时写一条支出流水。
     *
     * @param purchaseOrder 采购订单号（幂等键）
     * @param receiptCode   入库单号
     * @param supplier      供应商
     * @param amount        采购金额（不含税，元）
     * @param tax           税额（元）
     */
    @Transactional
    public String createPayableForPurchase(String purchaseOrder, String receiptCode, String supplier,
                                           BigDecimal amount, BigDecimal tax) {
        if (!StringUtils.hasText(supplier) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("跳过应付生成：供应商或金额不合法 po={} supplier={} amount={}", purchaseOrder, supplier, amount);
            return null;
        }
        if (StringUtils.hasText(purchaseOrder)) {
            FinPayable existing = payableMapper.selectOne(new LambdaQueryWrapper<FinPayable>()
                    .eq(FinPayable::getPurchaseOrder, purchaseOrder.trim()).last("LIMIT 1"));
            if (existing != null) {
                log.debug("应付已存在，跳过 po={} code={}", purchaseOrder, existing.getCode());
                return existing.getCode();
            }
        }

        BigDecimal taxValue = tax == null ? BigDecimal.ZERO : tax;
        BigDecimal payable = amount.add(taxValue);
        LocalDateTime now = LocalDateTime.now();
        FinPayable entity = new FinPayable();
        entity.setCode(dailyCode("AP", FinPayable.class.getSimpleName()));
        entity.setReceiptCode(StringUtils.hasText(receiptCode) ? receiptCode.trim() : "");
        entity.setSupplier(supplier.trim());
        entity.setPurchaseOrder(StringUtils.hasText(purchaseOrder) ? purchaseOrder.trim() : "");
        entity.setPurchaseAmount(amount);
        entity.setTax(taxValue);
        entity.setPayable(payable);
        entity.setPaid(BigDecimal.ZERO);
        entity.setPending(payable);
        entity.setMatched(0);
        entity.setDueDate(LocalDate.now().plusDays(30).toString());
        entity.setStatus("待匹配");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        payableMapper.insert(entity);

        writeRecord("采购成本", "WMS 采购", amount, "支出", "已完成", now);
        log.info("采购入库生成应付 po={} code={} payable={}", purchaseOrder, entity.getCode(), payable);
        return entity.getCode();
    }

    /**
     * 出库完成但金额为 0 时也留一条 0 元流水，方便「业务流程」页体现单据链。
     * 金额为 0 的应收没有财务意义，所以只写流水不建应收。
     */
    @Transactional
    public void recordSalesWithoutAmount(String salesOrder, String module, LocalDateTime at) {
        if (!StringUtils.hasText(salesOrder)) return;
        writeRecord("销售收入", module, BigDecimal.ZERO, "收入", "待确认",
                at == null ? LocalDateTime.now() : at);
    }

    private void writeRecord(String bizType, String module, BigDecimal amount,
                             String direction, String status, LocalDateTime now) {
        FinRecord record = new FinRecord();
        record.setCode(dailyCode("FR", FinRecord.class.getSimpleName()));
        record.setBizType(bizType);
        record.setModule(module);
        record.setAmount(amount);
        record.setDirection(direction);
        record.setStatus(status);
        record.setDate(now.toLocalDate().toString());
        record.setCreateTime(now);
        recordMapper.insert(record);
    }

    /**
     * 生成「前缀 + 日期 + 当日 3 位流水」的单号，例如 AR20261026001。
     * 单号只用于展示与检索，不承担唯一约束，因此按当日计数即可。
     */
    private String dailyCode(String prefix, String tableType) {
        String day = LocalDate.now().format(DAY);
        String head = prefix + day;
        long seq;
        if ("FinReceivable".equals(tableType)) {
            seq = receivableMapper.selectCount(new LambdaQueryWrapper<FinReceivable>()
                    .likeRight(FinReceivable::getCode, head)) + 1;
        } else if ("FinPayable".equals(tableType)) {
            seq = payableMapper.selectCount(new LambdaQueryWrapper<FinPayable>()
                    .likeRight(FinPayable::getCode, head)) + 1;
        } else {
            seq = recordMapper.selectCount(new LambdaQueryWrapper<FinRecord>()
                    .likeRight(FinRecord::getCode, head)) + 1;
        }
        return head + String.format("%03d", seq);
    }
}
