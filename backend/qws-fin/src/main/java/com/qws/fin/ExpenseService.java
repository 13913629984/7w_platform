package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinExpense;
import com.qws.common.mapper.FinExpenseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qws.fin.FinSupport.decimal;
import static com.qws.fin.FinSupport.defaultText;
import static com.qws.fin.FinSupport.nz;
import static com.qws.fin.FinSupport.text;

/**
 * 费用管理服务：费用申请的列表筛选、按类型汇总统计、新增与审批。
 */
@Service
public class ExpenseService {

    private final FinExpenseMapper expenseMapper;

    public ExpenseService(FinExpenseMapper expenseMapper) {
        this.expenseMapper = expenseMapper;
    }

    public List<Map<String, Object>> list(String startMonth, String endMonth, String type) {
        LambdaQueryWrapper<FinExpense> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) wrapper.eq(FinExpense::getType, type);
        if (StringUtils.hasText(startMonth)) wrapper.ge(FinExpense::getDate, startMonth + "-01");
        if (StringUtils.hasText(endMonth)) wrapper.le(FinExpense::getDate, endMonth + "-31");
        wrapper.orderByDesc(FinExpense::getDate).orderByDesc(FinExpense::getId);
        return expenseMapper.selectList(wrapper).stream().map(this::row).toList();
    }

    public Map<String, Object> stats() {
        List<FinExpense> all = expenseMapper.selectList(null);
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal manage = BigDecimal.ZERO;
        BigDecimal rd = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (FinExpense e : all) {
            BigDecimal amount = nz(e.getAmount());
            total = total.add(amount);
            switch (text(e.getType())) {
                case "销售费用" -> sales = sales.add(amount);
                case "管理费用" -> manage = manage.add(amount);
                case "研发费用" -> rd = rd.add(amount);
                default -> { }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("sales", sales);
        result.put("manage", manage);
        result.put("rd", rd);
        result.put("total", total);
        return result;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String item = text(request.get("item"));
        BigDecimal amount = decimal(request.get("amount"));
        if (!StringUtils.hasText(item)) throw new IllegalArgumentException("请填写费用项目");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("费用金额必须大于0");

        LocalDateTime now = LocalDateTime.now();
        FinExpense entity = new FinExpense();
        entity.setCode(text(request.getOrDefault("code", "EXP-" + LocalDate.now() + "-" + System.currentTimeMillis() % 1000)));
        entity.setType(defaultText(request.get("type"), "管理费用"));
        entity.setModule(defaultText(request.get("module"), "系统"));
        entity.setItem(item);
        entity.setAmount(amount);
        entity.setDate(defaultText(request.get("date"), LocalDate.now().toString()));
        entity.setApplicant(defaultText(request.get("applicant"), "admin"));
        entity.setApproved(0);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        expenseMapper.insert(entity);
        return row(entity);
    }

    @Transactional
    public Map<String, Object> approve(Map<String, Object> request) {
        Object idValue = request.get("id");
        if (!(idValue instanceof Number number)) throw new IllegalArgumentException("缺少费用ID");
        FinExpense expense = expenseMapper.selectById(number.longValue());
        if (expense == null) throw new IllegalArgumentException("费用单不存在");
        expense.setApproved(1);
        expense.setUpdateTime(LocalDateTime.now());
        expenseMapper.updateById(expense);
        return row(expense);
    }

    private Map<String, Object> row(FinExpense e) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", e.getId());
        result.put("code", e.getCode());
        result.put("type", e.getType());
        result.put("module", e.getModule());
        result.put("item", e.getItem());
        result.put("amount", e.getAmount());
        result.put("date", e.getDate());
        result.put("applicant", e.getApplicant());
        result.put("approved", e.getApproved() != null && e.getApproved() == 1);
        return result;
    }
}
