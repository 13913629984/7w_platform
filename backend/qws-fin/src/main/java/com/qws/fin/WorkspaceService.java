package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinPayable;
import com.qws.common.entity.FinReceivable;
import com.qws.common.entity.FinRecord;
import com.qws.common.mapper.FinPayableMapper;
import com.qws.common.mapper.FinReceivableMapper;
import com.qws.common.mapper.FinRecordMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qws.fin.FinSupport.contains;
import static com.qws.fin.FinSupport.nz;
import static com.qws.fin.FinSupport.text;

/**
 * 财务聚合视图服务：基于财务流水、应收、应付数据，
 * 为工作台、应收应付总览、业务流程页面提供统计与列表数据。
 */
@Service
public class WorkspaceService {

    private final FinRecordMapper recordMapper;
    private final FinReceivableMapper receivableMapper;
    private final FinPayableMapper payableMapper;

    public WorkspaceService(FinRecordMapper recordMapper, FinReceivableMapper receivableMapper, FinPayableMapper payableMapper) {
        this.recordMapper = recordMapper;
        this.receivableMapper = receivableMapper;
        this.payableMapper = payableMapper;
    }

    public Map<String, Object> workspace() {
        List<FinRecord> records = recordMapper.selectList(new LambdaQueryWrapper<FinRecord>()
                .orderByDesc(FinRecord::getDate).orderByDesc(FinRecord::getId));
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (FinRecord r : records) {
            if ("收入".equals(text(r.getDirection()))) income = income.add(nz(r.getAmount()));
            else expense = expense.add(nz(r.getAmount()));
        }
        BigDecimal pendingReceipt = receivableMapper.selectList(null).stream()
                .map(r -> nz(r.getPending())).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("income", income);
        stats.put("expense", expense);
        stats.put("profit", income.subtract(expense));
        stats.put("pendingReceipt", pendingReceipt);

        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("records", records.stream().limit(8).map(this::recordRow).toList());
        return result;
    }

    public Map<String, Object> arap(String keyword) {
        List<Map<String, Object>> receivables = receivableMapper.selectList(new LambdaQueryWrapper<FinReceivable>()
                        .orderByDesc(FinReceivable::getId)).stream()
                .map(this::receivableRow)
                .filter(r -> !hasKeyword(keyword) || contains(r.get("customer"), keyword.toLowerCase()) || contains(r.get("code"), keyword.toLowerCase()))
                .toList();
        List<Map<String, Object>> payables = payableMapper.selectList(new LambdaQueryWrapper<FinPayable>()
                        .orderByDesc(FinPayable::getId)).stream()
                .map(this::payableRow)
                .filter(p -> !hasKeyword(keyword) || contains(p.get("supplier"), keyword.toLowerCase()) || contains(p.get("code"), keyword.toLowerCase()))
                .toList();

        BigDecimal arTotal = receivables.stream().map(r -> nz((BigDecimal) r.get("pending"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal apTotal = payables.stream().map(p -> nz((BigDecimal) p.get("pending"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        long arOverdue = receivables.stream().filter(r -> "逾期".equals(text(r.get("status")))).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("arTotal", arTotal);
        stats.put("apTotal", apTotal);
        stats.put("arCount", receivables.size());
        stats.put("apCount", payables.size());
        stats.put("arOverdue", arOverdue);

        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("receivables", receivables);
        result.put("payables", payables);
        return result;
    }

    public List<Map<String, Object>> businessFlow(String keyword) {
        return recordMapper.selectList(new LambdaQueryWrapper<FinRecord>()
                        .orderByDesc(FinRecord::getDate).orderByDesc(FinRecord::getId)).stream()
                .map(this::recordRow)
                .filter(r -> !hasKeyword(keyword) || contains(r.get("code"), keyword.toLowerCase()) || contains(r.get("bizType"), keyword.toLowerCase()))
                .toList();
    }

    public Map<String, Object> businessFlowStats() {
        List<FinRecord> records = recordMapper.selectList(null);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int confirmed = 0;
        int processing = 0;
        for (FinRecord r : records) {
            if ("收入".equals(text(r.getDirection()))) income = income.add(nz(r.getAmount()));
            else expense = expense.add(nz(r.getAmount()));
            if ("处理中".equals(text(r.getStatus()))) processing++; else confirmed++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("income", income);
        result.put("expense", expense);
        result.put("total", records.size());
        result.put("confirmed", confirmed);
        result.put("processing", processing);
        return result;
    }

    private boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.trim().isEmpty();
    }

    private Map<String, Object> recordRow(FinRecord r) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("code", r.getCode());
        result.put("bizType", r.getBizType());
        result.put("module", r.getModule());
        result.put("amount", r.getAmount());
        result.put("direction", r.getDirection());
        result.put("status", r.getStatus());
        result.put("date", r.getDate());
        return result;
    }

    private Map<String, Object> receivableRow(FinReceivable r) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("code", r.getCode());
        result.put("customer", r.getCustomer());
        result.put("salesOrder", r.getSalesOrder());
        result.put("contract", r.getContract());
        result.put("contractAmount", r.getAmount());
        result.put("received", r.getWrittenOff());
        result.put("pending", r.getPending());
        result.put("dueDate", r.getDueDate());
        result.put("status", r.getStatus());
        return result;
    }

    private Map<String, Object> payableRow(FinPayable p) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", p.getId());
        result.put("code", p.getCode());
        result.put("supplier", p.getSupplier());
        result.put("purchaseOrder", p.getPurchaseOrder());
        result.put("payableAmount", p.getPayable());
        result.put("paid", p.getPaid());
        result.put("pending", p.getPending());
        result.put("dueDate", p.getDueDate());
        result.put("status", p.getStatus());
        return result;
    }
}
