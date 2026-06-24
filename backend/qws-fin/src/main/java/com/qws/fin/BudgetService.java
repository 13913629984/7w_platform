package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinBudget;
import com.qws.common.mapper.FinBudgetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qws.fin.FinSupport.decimal;
import static com.qws.fin.FinSupport.nz;
import static com.qws.fin.FinSupport.text;

/**
 * 预算管理服务：部门预算的列表、执行率统计、图表数据（执行进度 / 预算对比）与增改。
 */
@Service
public class BudgetService {

    private final FinBudgetMapper budgetMapper;

    public BudgetService(FinBudgetMapper budgetMapper) {
        this.budgetMapper = budgetMapper;
    }

    public List<Map<String, Object>> list() {
        return budgetMapper.selectList(new LambdaQueryWrapper<FinBudget>()
                .orderByAsc(FinBudget::getId)).stream().map(this::row).toList();
    }

    public Map<String, Object> stats() {
        List<FinBudget> all = budgetMapper.selectList(null);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal executed = BigDecimal.ZERO;
        int overBudget = 0;
        for (FinBudget b : all) {
            total = total.add(nz(b.getBudget()));
            executed = executed.add(nz(b.getExecuted()));
            if (nz(b.getRate()).compareTo(BigDecimal.valueOf(90)) >= 0) overBudget++;
        }
        BigDecimal balance = total.subtract(executed);
        BigDecimal rate = total.signum() == 0 ? BigDecimal.ZERO
                : executed.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("executed", executed);
        result.put("balance", balance);
        result.put("rate", rate);
        result.put("overBudget", overBudget);
        return result;
    }

    public Map<String, Object> charts() {
        List<FinBudget> all = budgetMapper.selectList(new LambdaQueryWrapper<FinBudget>().orderByAsc(FinBudget::getId));
        List<Map<String, Object>> progress = new ArrayList<>();
        List<Map<String, Object>> compare = new ArrayList<>();
        for (FinBudget b : all) {
            BigDecimal budget = nz(b.getBudget());
            BigDecimal executed = nz(b.getExecuted());
            Map<String, Object> p = new HashMap<>();
            p.put("name", shortName(b.getDept()));
            p.put("executed", executed);
            p.put("remain", budget.subtract(executed).max(BigDecimal.ZERO));
            progress.add(p);

            Map<String, Object> c = new HashMap<>();
            c.put("name", shortName(b.getDept()));
            c.put("budget", budget);
            c.put("actual", executed);
            compare.add(c);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("progress", progress);
        result.put("compare", compare);
        return result;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String dept = text(request.get("dept"));
        BigDecimal budget = decimal(request.get("budget"));
        if (!StringUtils.hasText(dept)) throw new IllegalArgumentException("请填写部门");
        if (budget.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("预算金额必须大于0");
        BigDecimal executed = decimal(request.get("executed"));

        LocalDateTime now = LocalDateTime.now();
        FinBudget entity = new FinBudget();
        entity.setDept(dept);
        entity.setItem(text(request.getOrDefault("item", dept + "预算")));
        entity.setBudget(budget);
        entity.setExecuted(executed);
        entity.setRate(rate(budget, executed));
        entity.setYear(toInt(request.get("year"), now.getYear()));
        entity.setMonth(toInt(request.get("month"), now.getMonthValue()));
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        budgetMapper.insert(entity);
        return row(entity);
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> request) {
        Object idValue = request.get("id");
        if (!(idValue instanceof Number number)) throw new IllegalArgumentException("缺少预算ID");
        FinBudget budget = budgetMapper.selectById(number.longValue());
        if (budget == null) throw new IllegalArgumentException("预算不存在");
        if (request.containsKey("budget")) budget.setBudget(decimal(request.get("budget")));
        if (request.containsKey("executed")) budget.setExecuted(decimal(request.get("executed")));
        if (request.containsKey("item")) budget.setItem(text(request.get("item")));
        budget.setRate(rate(nz(budget.getBudget()), nz(budget.getExecuted())));
        budget.setUpdateTime(LocalDateTime.now());
        budgetMapper.updateById(budget);
        return row(budget);
    }

    private BigDecimal rate(BigDecimal budget, BigDecimal executed) {
        return budget.signum() == 0 ? BigDecimal.ZERO
                : executed.multiply(BigDecimal.valueOf(100)).divide(budget, 1, RoundingMode.HALF_UP);
    }

    private String shortName(String dept) {
        String value = text(dept);
        return value.endsWith("部") ? value.substring(0, value.length() - 1) : value;
    }

    private Integer toInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        return defaultValue;
    }

    private Map<String, Object> row(FinBudget b) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", b.getId());
        result.put("dept", b.getDept());
        result.put("item", b.getItem());
        result.put("budget", b.getBudget());
        result.put("executed", b.getExecuted());
        result.put("rate", b.getRate());
        result.put("year", b.getYear());
        result.put("month", b.getMonth());
        return result;
    }
}
