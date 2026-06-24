package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinPayable;
import com.qws.common.mapper.FinPayableMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qws.fin.FinSupport.decimal;
import static com.qws.fin.FinSupport.nz;
import static com.qws.fin.FinSupport.text;

/**
 * 应付款管理服务：维护应付单据、待匹配收货单、发票匹配与应付统计。
 */
@Service
public class PayableService {

    private final FinPayableMapper payableMapper;

    public PayableService(FinPayableMapper payableMapper) {
        this.payableMapper = payableMapper;
    }

    public List<Map<String, Object>> list(String keyword, String status) {
        LambdaQueryWrapper<FinPayable> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(FinPayable::getSupplier, value).or().like(FinPayable::getCode, value));
        }
        if (StringUtils.hasText(status)) wrapper.eq(FinPayable::getStatus, status);
        wrapper.orderByDesc(FinPayable::getCreateTime).orderByDesc(FinPayable::getId);
        return payableMapper.selectList(wrapper).stream().map(this::row).toList();
    }

    public List<Map<String, Object>> pendingReceipts() {
        // 待匹配发票：未匹配且状态为待匹配的应付单据，作为收货待匹配清单展示
        return payableMapper.selectList(new LambdaQueryWrapper<FinPayable>()
                        .eq(FinPayable::getMatched, 0)
                        .orderByDesc(FinPayable::getCreateTime)).stream()
                .map(p -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", p.getReceiptCode());
                    result.put("purchaseOrder", p.getPurchaseOrder());
                    result.put("supplier", p.getSupplier());
                    result.put("amount", p.getPurchaseAmount());
                    result.put("tax", p.getTax());
                    result.put("payable", p.getPayable());
                    return result;
                }).toList();
    }

    public Map<String, Object> stats() {
        List<FinPayable> all = payableMapper.selectList(null);
        BigDecimal total = BigDecimal.ZERO;
        int pendingMatch = 0;
        int matched = 0;
        int overdue = 0;
        for (FinPayable p : all) {
            total = total.add(nz(p.getPayable()));
            boolean isMatched = p.getMatched() != null && p.getMatched() == 1;
            if (isMatched) matched++; else pendingMatch++;
            if ("逾期".equals(text(p.getStatus()))) overdue++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("count", all.size());
        result.put("pendingMatch", pendingMatch);
        result.put("matched", matched);
        result.put("overdue", overdue);
        return result;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String supplier = text(request.get("supplier"));
        BigDecimal purchaseAmount = decimal(request.get("purchaseAmount"));
        if (!StringUtils.hasText(supplier)) throw new IllegalArgumentException("请填写供应商");
        if (purchaseAmount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("采购金额必须大于0");
        BigDecimal tax = decimal(request.get("tax"));
        BigDecimal payable = purchaseAmount.add(tax);

        LocalDateTime now = LocalDateTime.now();
        FinPayable entity = new FinPayable();
        entity.setCode(text(request.getOrDefault("code", "AP" + now.toLocalDate().toString().replace("-", "") + System.currentTimeMillis() % 1000)));
        entity.setReceiptCode(text(request.get("receiptCode")));
        entity.setSupplier(supplier);
        entity.setPurchaseOrder(text(request.get("purchaseOrder")));
        entity.setPurchaseAmount(purchaseAmount);
        entity.setTax(tax);
        entity.setPayable(payable);
        entity.setPaid(BigDecimal.ZERO);
        entity.setPending(payable);
        entity.setMatched(0);
        entity.setDueDate(text(request.get("dueDate")));
        entity.setStatus("待匹配");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        payableMapper.insert(entity);
        return row(entity);
    }

    @Transactional
    public Map<String, Object> match(Map<String, Object> request) {
        Long id = toId(request.get("id"));
        FinPayable payable = payableMapper.selectById(id);
        if (payable == null) throw new IllegalArgumentException("应付单不存在");
        payable.setMatched(1);
        if ("待匹配".equals(text(payable.getStatus()))) payable.setStatus("待付款");
        payable.setUpdateTime(LocalDateTime.now());
        payableMapper.updateById(payable);
        return row(payable);
    }

    private Map<String, Object> row(FinPayable p) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", p.getId());
        result.put("code", p.getCode());
        result.put("receiptCode", p.getReceiptCode());
        result.put("supplier", p.getSupplier());
        result.put("purchaseOrder", p.getPurchaseOrder());
        result.put("purchaseAmount", p.getPurchaseAmount());
        result.put("tax", p.getTax());
        result.put("payable", p.getPayable());
        result.put("paid", p.getPaid());
        result.put("pending", p.getPending());
        result.put("matched", p.getMatched() != null && p.getMatched() == 1);
        result.put("dueDate", p.getDueDate());
        result.put("status", p.getStatus());
        return result;
    }

    private Long toId(Object value) {
        if (value instanceof Number number) return number.longValue();
        throw new IllegalArgumentException("缺少单据ID");
    }
}
