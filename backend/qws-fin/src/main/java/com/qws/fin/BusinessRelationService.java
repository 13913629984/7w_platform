package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.CrmOrder;
import com.qws.common.entity.FinPayable;
import com.qws.common.entity.FinPayment;
import com.qws.common.entity.FinReceiptRecord;
import com.qws.common.entity.FinReceivable;
import com.qws.common.entity.InboundOrder;
import com.qws.common.entity.OutboundOrder;
import com.qws.common.entity.PurchaseOrder;
import com.qws.common.mapper.CrmOrderMapper;
import com.qws.common.mapper.FinPayableMapper;
import com.qws.common.mapper.FinPaymentMapper;
import com.qws.common.mapper.FinReceiptRecordMapper;
import com.qws.common.mapper.FinReceivableMapper;
import com.qws.common.mapper.InboundOrderMapper;
import com.qws.common.mapper.OutboundOrderMapper;
import com.qws.common.mapper.PurchaseOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 「业务流程」页的跨模块单据关联查询。
 *
 * <p>平台是单库多服务部署，所以这里可以直接 order 表 join 出入库表 join 财务表，
 * 而不必让前端拼一堆跨子系统请求。核心是两条链：
 * <ul>
 *   <li>销售链：CRM 订单 → WMS 出库单 → 应收款 → 回款单</li>
 *   <li>采购链：WMS 采购订单 → WMS 入库单 → 应付款 → 付款单</li>
 * </ul>
 *
 * <p>关联键：销售订单号、采购订单号、入库单号、单据号。任何一环缺失都留空（前端显示 '-'），
 * 这样能直观看出流程卡在哪一环。
 */
@Service
public class BusinessRelationService {

    private final CrmOrderMapper crmOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final FinReceivableMapper receivableMapper;
    private final FinReceiptRecordMapper receiptRecordMapper;
    private final FinPayableMapper payableMapper;
    private final FinPaymentMapper paymentMapper;

    public BusinessRelationService(CrmOrderMapper crmOrderMapper,
                                   OutboundOrderMapper outboundOrderMapper,
                                   InboundOrderMapper inboundOrderMapper,
                                   PurchaseOrderMapper purchaseOrderMapper,
                                   FinReceivableMapper receivableMapper,
                                   FinReceiptRecordMapper receiptRecordMapper,
                                   FinPayableMapper payableMapper,
                                   FinPaymentMapper paymentMapper) {
        this.crmOrderMapper = crmOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.receivableMapper = receivableMapper;
        this.receiptRecordMapper = receiptRecordMapper;
        this.payableMapper = payableMapper;
        this.paymentMapper = paymentMapper;
    }

    /**
     * 生成销售链 + 采购链的关联明细。
     *
     * @param keyword 可选，匹配任一单据号/客户/供应商
     */
    public List<Map<String, Object>> relations(String keyword) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(salesRelations());
        rows.addAll(purchaseRelations());
        // 新单据排前面（按订单号倒序近似，单据号里带日期）
        rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("salesOrder") == null
                ? r.get("purchaseOrder") : r.get("salesOrder"))).reversed());
        if (!StringUtils.hasText(keyword)) {
            return rows;
        }
        String kw = keyword.trim().toLowerCase();
        return rows.stream().filter(r -> String.valueOf(r).toLowerCase().contains(kw)).toList();
    }

    /** 销售链：CRM 订单 → 出库单 → 应收 → 回款 */
    private List<Map<String, Object>> salesRelations() {
        List<CrmOrder> orders = crmOrderMapper.selectList(new LambdaQueryWrapper<CrmOrder>()
                .orderByDesc(CrmOrder::getId).last("LIMIT 200"));
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<String, OutboundOrder> outboundByOrderNo = outboundOrderMapper.selectList(null).stream()
                .filter(o -> StringUtils.hasText(o.getOrderNo()))
                .collect(Collectors.toMap(OutboundOrder::getOrderNo, Function.identity(), (a, b) -> a));
        Map<String, FinReceivable> receivableBySalesOrder = receivableMapper.selectList(null).stream()
                .filter(r -> StringUtils.hasText(r.getSalesOrder()))
                .collect(Collectors.toMap(FinReceivable::getSalesOrder, Function.identity(), (a, b) -> a));
        // 回款单按应收单号索引（一张应收可能分多次回款，取最近一笔做代表）
        Map<String, FinReceiptRecord> receiptByReceivableCode = receiptRecordMapper.selectList(null).stream()
                .filter(r -> StringUtils.hasText(r.getReceivableCode()))
                .collect(Collectors.toMap(FinReceiptRecord::getReceivableCode, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CrmOrder order : orders) {
            OutboundOrder outbound = outboundByOrderNo.get(String.valueOf(order.getOrderNo()));
            FinReceivable receivable = receivableBySalesOrder.get(String.valueOf(order.getOrderNo()));
            FinReceiptRecord receipt = receivable == null ? null : receiptByReceivableCode.get(receivable.getCode());

            Map<String, Object> row = new HashMap<>();
            row.put("bizType", "销售业务");
            row.put("salesOrder", order.getOrderNo());
            row.put("purchaseOrder", "-");
            row.put("stockDoc", outbound != null ? outbound.getCode() : "-");
            row.put("arap", receivable != null ? receivable.getCode() : "-");
            row.put("payment", receipt != null ? receipt.getCode() : "-");
            row.put("status", salesStatus(order, outbound, receivable));
            row.put("amount", order.getAmount());
            row.put("partner", order.getCustomerName());
            row.put("date", order.getSignDate() == null ? "" : order.getSignDate().toString());
            rows.add(row);
        }
        return rows;
    }

    /** 采购链：采购订单 → 入库单 → 应付 → 付款 */
    private List<Map<String, Object>> purchaseRelations() {
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .orderByDesc(PurchaseOrder::getId).last("LIMIT 200"));
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<String, InboundOrder> inboundByPoCode = inboundOrderMapper.selectList(null).stream()
                .filter(o -> StringUtils.hasText(o.getPoCode()))
                .collect(Collectors.toMap(InboundOrder::getPoCode, Function.identity(), (a, b) -> a));
        Map<String, FinPayable> payableByPo = payableMapper.selectList(null).stream()
                .filter(p -> StringUtils.hasText(p.getPurchaseOrder()))
                .collect(Collectors.toMap(FinPayable::getPurchaseOrder, Function.identity(), (a, b) -> a));
        Map<String, FinPayment> paymentByApCode = paymentMapper.selectList(null).stream()
                .filter(p -> StringUtils.hasText(p.getApCode()))
                .collect(Collectors.toMap(FinPayment::getApCode, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (PurchaseOrder order : orders) {
            InboundOrder inbound = inboundByPoCode.get(String.valueOf(order.getCode()));
            FinPayable payable = payableByPo.get(String.valueOf(order.getCode()));
            FinPayment payment = payable == null ? null : paymentByApCode.get(payable.getCode());

            Map<String, Object> row = new HashMap<>();
            row.put("bizType", "采购业务");
            row.put("salesOrder", "-");
            row.put("purchaseOrder", order.getCode());
            row.put("stockDoc", inbound != null ? inbound.getCode() : (StringUtils.hasText(order.getInboundCode()) ? order.getInboundCode() : "-"));
            row.put("arap", payable != null ? payable.getCode() : "-");
            row.put("payment", payment != null ? payment.getCode() : "-");
            row.put("status", purchaseStatus(order, inbound, payable, payment));
            row.put("amount", order.getTotalAmount());
            row.put("partner", order.getSupplier());
            row.put("date", order.getDeliveryDate() == null ? "" : order.getDeliveryDate().toString());
            rows.add(row);
        }
        return rows;
    }

    /** 销售链状态：按最下游已完成的环节命名，方便一眼看出卡点 */
    private String salesStatus(CrmOrder order, OutboundOrder outbound, FinReceivable receivable) {
        if (receivable != null) {
            if ("已核销".equals(FinSupport.text(receivable.getStatus()))) return "已回款";
            return "待回款";
        }
        if (outbound != null) {
            return "completed".equals(FinSupport.text(outbound.getStatus())) ? "已出库" : "待出库";
        }
        return FinSupport.text(order.getStatus());
    }

    /** 采购链状态 */
    private String purchaseStatus(PurchaseOrder order, InboundOrder inbound, FinPayable payable, FinPayment payment) {
        if (payment != null) {
            return "已付款".equals(FinSupport.text(payment.getStatus())) ? "已付款" : "付款中";
        }
        if (payable != null) {
            return payable.getMatched() != null && payable.getMatched() == 1 ? "待付款" : "待匹配";
        }
        if (inbound != null) {
            return "completed".equals(FinSupport.text(inbound.getStatus())) ? "已入库" : "入库中";
        }
        return FinSupport.text(order.getStatus());
    }

    /** 供统计使用：关联完整度（五项单据齐全的比例） */
    public Map<String, Object> completeness(String keyword) {
        List<Map<String, Object>> rows = relations(keyword);
        int complete = 0;
        for (Map<String, Object> row : rows) {
            boolean full = !"-".equals(row.get("stockDoc")) && !"-".equals(row.get("arap")) && !"-".equals(row.get("payment"));
            if (full) complete++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("complete", complete);
        result.put("pending", rows.size() - complete);
        return result;
    }

    /** 金额合计，避免前端重复算 */
    public BigDecimal totalAmount(List<Map<String, Object>> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object amount = row.get("amount");
            if (amount instanceof BigDecimal value) total = total.add(value);
        }
        return total;
    }
}
