package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinPayment;
import com.qws.common.mapper.FinPaymentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qws.fin.FinSupport.decimal;
import static com.qws.fin.FinSupport.defaultText;
import static com.qws.fin.FinSupport.nz;
import static com.qws.fin.FinSupport.text;

/**
 * 付款管理服务：付款单的创建、审批流转（待审批→审批中→已通过→已付款 / 已驳回）与统计。
 */
@Service
public class PaymentService {

    private static final DateTimeFormatter APPLY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FinPaymentMapper paymentMapper;

    public PaymentService(FinPaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    public List<Map<String, Object>> list(String keyword, String status) {
        LambdaQueryWrapper<FinPayment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(FinPayment::getPayee, value).or().like(FinPayment::getCode, value));
        }
        if (StringUtils.hasText(status)) wrapper.eq(FinPayment::getStatus, status);
        wrapper.orderByDesc(FinPayment::getCreateTime).orderByDesc(FinPayment::getId);
        return paymentMapper.selectList(wrapper).stream().map(this::row).toList();
    }

    public Map<String, Object> stats() {
        List<FinPayment> all = paymentMapper.selectList(null);
        BigDecimal monthTotal = BigDecimal.ZERO;
        int paid = 0;
        int pendingApprove = 0;
        int approving = 0;
        int approved = 0;
        int rejected = 0;
        for (FinPayment p : all) {
            monthTotal = monthTotal.add(nz(p.getAmount()));
            switch (text(p.getStatus())) {
                case "已付款" -> paid++;
                case "待审批" -> pendingApprove++;
                case "审批中" -> approving++;
                case "已通过" -> approved++;
                case "已驳回" -> rejected++;
                default -> { }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("monthTotal", monthTotal);
        result.put("paid", paid);
        result.put("pendingApprove", pendingApprove);
        result.put("approving", approving);
        result.put("approved", approved);
        result.put("rejected", rejected);
        return result;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String payee = text(request.get("payee"));
        BigDecimal amount = decimal(request.get("amount"));
        if (!StringUtils.hasText(payee)) throw new IllegalArgumentException("请填写收款方");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("付款金额必须大于0");

        LocalDateTime now = LocalDateTime.now();
        FinPayment entity = new FinPayment();
        entity.setCode(text(request.getOrDefault("code", "PAY" + now.toLocalDate().toString().replace("-", "") + System.currentTimeMillis() % 1000)));
        entity.setType(defaultText(request.get("type"), "采购付款"));
        entity.setApCode(text(request.get("apCode")));
        entity.setPayee(payee);
        entity.setAmount(amount);
        entity.setMethod(defaultText(request.get("method"), "银行转账"));
        entity.setApplyDate(now.format(APPLY_FORMAT));
        entity.setStatus("待审批");
        entity.setRemark(text(request.get("remark")));
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        paymentMapper.insert(entity);
        return row(entity);
    }

    @Transactional
    public Map<String, Object> approve(Map<String, Object> request) {
        FinPayment payment = load(request.get("id"));
        String next = switch (text(payment.getStatus())) {
            case "待审批" -> "审批中";
            case "审批中" -> "已通过";
            case "已通过" -> "已付款";
            default -> throw new IllegalStateException("当前状态无法继续审批");
        };
        payment.setStatus(next);
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);
        return row(payment);
    }

    @Transactional
    public Map<String, Object> reject(Map<String, Object> request) {
        FinPayment payment = load(request.get("id"));
        if ("已付款".equals(text(payment.getStatus()))) throw new IllegalStateException("已付款单据不可驳回");
        payment.setStatus("已驳回");
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);
        return row(payment);
    }

    private FinPayment load(Object idValue) {
        if (!(idValue instanceof Number number)) throw new IllegalArgumentException("缺少单据ID");
        FinPayment payment = paymentMapper.selectById(number.longValue());
        if (payment == null) throw new IllegalArgumentException("付款单不存在");
        return payment;
    }

    private Map<String, Object> row(FinPayment p) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", p.getId());
        result.put("code", p.getCode());
        result.put("type", p.getType());
        result.put("apCode", p.getApCode());
        result.put("payee", p.getPayee());
        result.put("amount", p.getAmount());
        result.put("method", p.getMethod());
        result.put("applyDate", p.getApplyDate());
        result.put("status", p.getStatus());
        result.put("remark", p.getRemark());
        return result;
    }
}
