package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinReceiptRecord;
import com.qws.common.entity.FinReceivable;
import com.qws.common.mapper.FinReceiptRecordMapper;
import com.qws.common.mapper.FinReceivableMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qws.fin.FinSupport.contains;
import static com.qws.fin.FinSupport.decimal;
import static com.qws.fin.FinSupport.nz;
import static com.qws.fin.FinSupport.text;

/**
 * 应收款管理服务：维护应收单据、回款核销与开票状态，并提供应收相关的统计聚合。
 */
@Service
public class ReceivableService {

    private final FinReceivableMapper receivableMapper;
    private final FinReceiptRecordMapper recordMapper;

    public ReceivableService(FinReceivableMapper receivableMapper, FinReceiptRecordMapper recordMapper) {
        this.receivableMapper = receivableMapper;
        this.recordMapper = recordMapper;
    }

    public List<Map<String, Object>> list(String keyword, String status, String customer) {
        LambdaQueryWrapper<FinReceivable> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(FinReceivable::getCustomer, value).or().like(FinReceivable::getCode, value));
        }
        if (StringUtils.hasText(status)) wrapper.eq(FinReceivable::getStatus, status);
        if (StringUtils.hasText(customer)) wrapper.eq(FinReceivable::getCustomer, customer);
        wrapper.orderByDesc(FinReceivable::getCreateTime).orderByDesc(FinReceivable::getId);
        return receivableMapper.selectList(wrapper).stream().map(this::row).toList();
    }

    public List<Map<String, Object>> records(String keyword) {
        LambdaQueryWrapper<FinReceiptRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FinReceiptRecord::getCreateTime).orderByDesc(FinReceiptRecord::getId);
        List<Map<String, Object>> rows = recordMapper.selectList(wrapper).stream().map(this::recordRow).toList();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream().filter(r -> contains(r.get("customer"), kw) || contains(r.get("code"), kw)).toList();
        }
        return rows;
    }

    public Map<String, Object> stats() {
        List<FinReceivable> all = receivableMapper.selectList(null);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal writtenOff = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        for (FinReceivable r : all) {
            total = total.add(nz(r.getAmount()));
            writtenOff = writtenOff.add(nz(r.getWrittenOff()));
            pending = pending.add(nz(r.getPending()));
        }
        BigDecimal monthReceipt = recordMapper.selectList(null).stream()
                .map(rec -> nz(rec.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("writtenOff", writtenOff);
        result.put("pending", pending);
        result.put("monthReceipt", monthReceipt);
        result.put("count", all.size());
        return result;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String customer = text(request.get("customer"));
        BigDecimal amount = decimal(request.get("amount"));
        if (!StringUtils.hasText(customer)) throw new IllegalArgumentException("请填写客户名称");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("应收金额必须大于0");

        LocalDateTime now = LocalDateTime.now();
        FinReceivable entity = new FinReceivable();
        entity.setCode(text(request.getOrDefault("code", "AR" + now.toLocalDate().toString().replace("-", "") + System.currentTimeMillis() % 1000)));
        entity.setCustomer(customer);
        entity.setSalesOrder(text(request.get("salesOrder")));
        entity.setContract(text(request.get("contract")));
        entity.setAmount(amount);
        entity.setWrittenOff(BigDecimal.ZERO);
        entity.setPending(amount);
        entity.setInvoiced(toInt(request.get("invoiced")));
        entity.setDueDate(text(request.get("dueDate")));
        entity.setStatus("待核销");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        receivableMapper.insert(entity);
        return row(entity);
    }

    @Transactional
    public Map<String, Object> writeOff(Map<String, Object> request) {
        Long id = toId(request.get("id"));
        FinReceivable receivable = receivableMapper.selectById(id);
        if (receivable == null) throw new IllegalArgumentException("应收单不存在");
        BigDecimal amount = decimal(request.get("amount"));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("回款金额必须大于0");
        BigDecimal pending = nz(receivable.getPending());
        if (amount.compareTo(pending) > 0) throw new IllegalArgumentException("回款金额不能超过待核销金额");

        LocalDateTime now = LocalDateTime.now();
        BigDecimal writtenOff = nz(receivable.getWrittenOff()).add(amount);
        BigDecimal newPending = pending.subtract(amount);
        receivable.setWrittenOff(writtenOff);
        receivable.setPending(newPending);
        receivable.setStatus(newPending.compareTo(BigDecimal.ZERO) <= 0 ? "已核销" : "部分核销");
        receivable.setUpdateTime(now);
        receivableMapper.updateById(receivable);

        FinReceiptRecord record = new FinReceiptRecord();
        record.setCode("RK" + now.toLocalDate().toString().replace("-", "") + (System.currentTimeMillis() % 1000));
        record.setReceivableId(receivable.getId());
        record.setReceivableCode(receivable.getCode());
        record.setCustomer(receivable.getCustomer());
        record.setAmount(amount);
        record.setMethod(FinSupport.defaultText(request.get("method"), "银行转账"));
        record.setDate(FinSupport.defaultText(request.get("date"), LocalDate.now().toString()));
        record.setOperator(FinSupport.defaultText(request.get("operator"), "admin"));
        record.setCreateTime(now);
        recordMapper.insert(record);
        return row(receivable);
    }

    @Transactional
    public Map<String, Object> invoice(Map<String, Object> request) {
        Long id = toId(request.get("id"));
        FinReceivable receivable = receivableMapper.selectById(id);
        if (receivable == null) throw new IllegalArgumentException("应收单不存在");
        receivable.setInvoiced(1);
        receivable.setUpdateTime(LocalDateTime.now());
        receivableMapper.updateById(receivable);
        return row(receivable);
    }

    private Map<String, Object> row(FinReceivable r) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("code", r.getCode());
        result.put("customer", r.getCustomer());
        result.put("salesOrder", r.getSalesOrder());
        result.put("contract", r.getContract());
        result.put("amount", r.getAmount());
        result.put("writtenOff", r.getWrittenOff());
        result.put("pending", r.getPending());
        result.put("invoiced", r.getInvoiced() != null && r.getInvoiced() == 1);
        result.put("dueDate", r.getDueDate());
        result.put("status", r.getStatus());
        return result;
    }

    private Map<String, Object> recordRow(FinReceiptRecord r) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("code", r.getCode());
        result.put("customer", r.getCustomer());
        result.put("receivableCode", r.getReceivableCode());
        result.put("amount", r.getAmount());
        result.put("method", r.getMethod());
        result.put("date", r.getDate());
        result.put("operator", r.getOperator());
        return result;
    }

    private Integer toInt(Object value) {
        if (value instanceof Boolean b) return b ? 1 : 0;
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    private Long toId(Object value) {
        if (value instanceof Number number) return number.longValue();
        throw new IllegalArgumentException("缺少单据ID");
    }
}
