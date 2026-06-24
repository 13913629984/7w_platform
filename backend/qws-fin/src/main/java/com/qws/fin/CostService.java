package com.qws.fin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.FinCostAllocation;
import com.qws.common.entity.FinCostCollection;
import com.qws.common.mapper.FinCostAllocationMapper;
import com.qws.common.mapper.FinCostCollectionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.qws.fin.FinSupport.contains;
import static com.qws.fin.FinSupport.decimal;
import static com.qws.fin.FinSupport.defaultText;
import static com.qws.fin.FinSupport.nz;
import static com.qws.fin.FinSupport.text;

/**
 * 成本归集服务：成本归集单与费用分配的列表/筛选、统计卡、成本分布与月度趋势图表，
 * 以及归集、分配两类写操作。
 */
@Service
public class CostService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FinCostCollectionMapper collectionMapper;
    private final FinCostAllocationMapper allocationMapper;

    public CostService(FinCostCollectionMapper collectionMapper, FinCostAllocationMapper allocationMapper) {
        this.collectionMapper = collectionMapper;
        this.allocationMapper = allocationMapper;
    }

    public List<Map<String, Object>> collections(String keyword, String costType) {
        LambdaQueryWrapper<FinCostCollection> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(costType)) wrapper.eq(FinCostCollection::getCostType, costType);
        wrapper.orderByDesc(FinCostCollection::getCreateTime).orderByDesc(FinCostCollection::getId);
        List<Map<String, Object>> rows = collectionMapper.selectList(wrapper).stream().map(this::collectionRow).toList();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream().filter(r -> contains(r.get("code"), kw) || contains(r.get("source"), kw)).toList();
        }
        return rows;
    }

    public List<Map<String, Object>> allocations(String keyword, String costType) {
        LambdaQueryWrapper<FinCostAllocation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(costType)) wrapper.eq(FinCostAllocation::getCostType, costType);
        wrapper.orderByDesc(FinCostAllocation::getCreateTime).orderByDesc(FinCostAllocation::getId);
        List<Map<String, Object>> rows = allocationMapper.selectList(wrapper).stream().map(this::allocationRow).toList();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream().filter(r -> contains(r.get("code"), kw) || contains(r.get("object"), kw)).toList();
        }
        return rows;
    }

    public Map<String, Object> stats() {
        List<FinCostCollection> collections = collectionMapper.selectList(null);
        List<FinCostAllocation> allocations = allocationMapper.selectList(null);
        BigDecimal collected = collections.stream().map(c -> nz(c.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allocated = allocations.stream().map(a -> nz(a.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = collections.stream()
                .filter(c -> "待处理".equals(text(c.getStatus())))
                .map(c -> nz(c.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new HashMap<>();
        result.put("collected", collected);
        result.put("allocated", allocated);
        result.put("accumulated", collected.add(allocated));
        result.put("pending", pending);
        result.put("collectionCount", collections.size());
        result.put("allocationCount", allocations.size());
        return result;
    }

    public Map<String, Object> charts() {
        List<FinCostCollection> collections = collectionMapper.selectList(null);
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        for (FinCostCollection c : collections) {
            byType.merge(text(c.getCostType()), nz(c.getAmount()), BigDecimal::add);
        }
        List<Map<String, Object>> distribution = new ArrayList<>();
        byType.forEach((name, value) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", value);
            distribution.add(item);
        });
        Map<String, Object> result = new HashMap<>();
        result.put("distribution", distribution);
        return result;
    }

    @Transactional
    public Map<String, Object> collect(Map<String, Object> request) {
        String source = text(request.get("source"));
        BigDecimal amount = decimal(request.get("amount"));
        if (!StringUtils.hasText(source)) throw new IllegalArgumentException("请填写成本来源");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("归集金额必须大于0");

        LocalDateTime now = LocalDateTime.now();
        FinCostCollection entity = new FinCostCollection();
        entity.setCode(text(request.getOrDefault("code", "CC" + now.toLocalDate().toString().replace("-", "") + System.currentTimeMillis() % 1000)));
        entity.setCostType(defaultText(request.get("costType"), "原材料"));
        entity.setSource(source);
        entity.setAmount(amount);
        entity.setDate(defaultText(request.get("date"), LocalDate.now().toString()));
        entity.setStatus("待处理");
        entity.setCreateTime(now);
        collectionMapper.insert(entity);
        return collectionRow(entity);
    }

    @Transactional
    public Map<String, Object> allocate(Map<String, Object> request) {
        Object idValue = request.get("collectionId");
        if (!(idValue instanceof Number number)) throw new IllegalArgumentException("缺少归集单ID");
        FinCostCollection collection = collectionMapper.selectById(number.longValue());
        if (collection == null) throw new IllegalArgumentException("归集单不存在");
        String object = text(request.get("object"));
        BigDecimal amount = decimal(request.get("amount"));
        if (!StringUtils.hasText(object)) throw new IllegalArgumentException("请填写分配对象");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("分配金额必须大于0");

        LocalDateTime now = LocalDateTime.now();
        FinCostAllocation entity = new FinCostAllocation();
        entity.setCode("FP" + now.toLocalDate().toString().replace("-", "") + System.currentTimeMillis() % 1000);
        entity.setCollectionId(collection.getId());
        entity.setCollectionCode(collection.getCode());
        entity.setCostType(collection.getCostType());
        entity.setObject(object);
        entity.setAmount(amount);
        entity.setRatio(defaultText(request.get("ratio"), "100%"));
        entity.setDate(now.format(STAMP));
        entity.setCreateTime(now);
        allocationMapper.insert(entity);

        collection.setStatus("已分配");
        collectionMapper.updateById(collection);
        return allocationRow(entity);
    }

    private Map<String, Object> collectionRow(FinCostCollection c) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", c.getId());
        result.put("code", c.getCode());
        result.put("costType", c.getCostType());
        result.put("source", c.getSource());
        result.put("amount", c.getAmount());
        result.put("date", c.getDate());
        result.put("status", c.getStatus());
        return result;
    }

    private Map<String, Object> allocationRow(FinCostAllocation a) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", a.getId());
        result.put("code", a.getCode());
        result.put("collectionCode", a.getCollectionCode());
        result.put("costType", a.getCostType());
        result.put("object", a.getObject());
        result.put("amount", a.getAmount());
        result.put("ratio", a.getRatio());
        result.put("date", a.getDate());
        return result;
    }
}
