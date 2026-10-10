package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.Material;
import com.qws.common.entity.Warehouse;
import com.qws.common.entity.WarningRule;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.WarehouseMapper;
import com.qws.common.mapper.WarningRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存预警规则：支持全局规则（适用于所有物料）与物料级规则（针对指定物料）。
 */
@Service
public class WarningRuleService {

    private final WarningRuleMapper ruleMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    public WarningRuleService(WarningRuleMapper ruleMapper, MaterialMapper materialMapper, WarehouseMapper warehouseMapper) {
        this.ruleMapper = ruleMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public List<Map<String, Object>> list(String keyword, String scope, Integer enabled) {
        LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(scope)) wrapper.eq(WarningRule::getScope, scope);
        if (enabled != null) wrapper.eq(WarningRule::getEnabled, enabled);
        wrapper.orderByDesc(WarningRule::getUpdateTime).orderByDesc(WarningRule::getId);

        Map<Long, Material> materialMap = materialMap();
        Map<String, Warehouse> warehouseMap = warehouseMap();

        List<Map<String, Object>> rows = ruleMapper.selectList(wrapper).stream()
                .map(rule -> row(rule, materialMap.get(rule.getMaterialId()), warehouseMap.get(rule.getWarehouseCode())))
                .collect(Collectors.toList());

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream()
                    .filter(r -> contains(r.get("name"), kw)
                            || contains(r.get("materialSku"), kw)
                            || contains(r.get("materialName"), kw))
                    .collect(Collectors.toList());
        }
        return rows;
    }

    public Map<String, Object> detail(Long id) {
        WarningRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new IllegalArgumentException("预警规则不存在");
        return row(rule, materialMap().get(rule.getMaterialId()), warehouseMap().get(rule.getWarehouseCode()));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        WarningRule rule = new WarningRule();
        apply(rule, request, true);
        LocalDateTime now = LocalDateTime.now();
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        ruleMapper.insert(rule);
        return detail(rule.getId());
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> request) {
        Long id = readLong(request.get("id"));
        if (id == null) throw new IllegalArgumentException("缺少规则ID");
        WarningRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new IllegalArgumentException("预警规则不存在");
        apply(rule, request, false);
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        return detail(rule.getId());
    }

    @Transactional
    public Map<String, Object> toggle(Map<String, Object> request) {
        Long id = readLong(request.get("id"));
        if (id == null) throw new IllegalArgumentException("缺少规则ID");
        WarningRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new IllegalArgumentException("预警规则不存在");
        rule.setEnabled(Boolean.FALSE.equals(readBool(request.get("enabled"))) ? 0 : 1);
        rule.setUpdateTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        return detail(rule.getId());
    }

    @Transactional
    public void delete(Long id) {
        ruleMapper.deleteById(id);
    }

    /** 命中某个物料+仓库时优先使用的启用规则，无匹配返回 null。 */
    public WarningRule matchRule(List<WarningRule> rules, Long materialId, String warehouseCode) {
        String wh = warehouseCode == null ? "" : warehouseCode.trim();
        return rules.stream()
                .filter(rule -> appliesTo(rule, materialId, wh))
                .min(Comparator
                        .comparingInt((WarningRule r) -> "global".equals(r.getScope()) ? 1 : 0)
                        .thenComparingInt(r -> StringUtils.hasText(r.getWarehouseCode()) ? 0 : 1)
                        .thenComparing(r -> r.getId() == null ? 0L : -r.getId()))
                .orElse(null);
    }

    public List<WarningRule> enabledRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<WarningRule>()
                .eq(WarningRule::getEnabled, 1));
    }

    private boolean appliesTo(WarningRule rule, Long materialId, String warehouseCode) {
        if ("material".equals(rule.getScope())) {
            if (rule.getMaterialId() == null || !rule.getMaterialId().equals(materialId)) return false;
        }
        String ruleWh = rule.getWarehouseCode() == null ? "" : rule.getWarehouseCode().trim();
        return ruleWh.isEmpty() || ruleWh.equals(warehouseCode);
    }

    private void apply(WarningRule rule, Map<String, Object> request, boolean creating) {
        String scope = text(request.get("scope"));
        if (!"global".equals(scope) && !"material".equals(scope)) {
            throw new IllegalArgumentException("请选择适用范围");
        }
        rule.setScope(scope);

        if (creating) {
            rule.setName(requireName(request.get("name")));
        } else if (request.containsKey("name")) {
            rule.setName(requireName(request.get("name")));
        }

        if ("material".equals(scope)) {
            Long materialId = readLong(request.get("materialId"));
            if (materialId == null) throw new IllegalArgumentException("请选择适用物料");
            if (materialMapper.selectById(materialId) == null) throw new IllegalArgumentException("适用物料不存在");
            rule.setMaterialId(materialId);
        } else {
            rule.setMaterialId(null);
        }

        String warehouseCode = text(request.get("warehouseCode"));
        rule.setWarehouseCode(StringUtils.hasText(warehouseCode) ? warehouseCode : null);

        int low = readInt(request.get("lowThreshold"));
        int medium = readInt(request.get("mediumThreshold"));
        int high = readInt(request.get("highThreshold"));
        if (low < 0 || medium < 0 || high < 0) throw new IllegalArgumentException("阈值不能为负数");
        if (low < medium || medium < high) throw new IllegalArgumentException("阈值需满足：高危 ≤ 中危 ≤ 低危");
        rule.setLowThreshold(low);
        rule.setMediumThreshold(medium);
        rule.setHighThreshold(high);

        if (request.containsKey("remark")) {
            String remark = text(request.get("remark"));
            rule.setRemark(StringUtils.hasText(remark) ? remark : null);
        }
        Boolean enabled = readBool(request.get("enabled"));
        if (creating) {
            // 默认启用
            rule.setEnabled(Boolean.FALSE.equals(enabled) ? 0 : 1);
        } else if (request.containsKey("enabled")) {
            rule.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        }

        ensureUnique(rule);
    }

    private void ensureUnique(WarningRule rule) {
        LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<WarningRule>()
                .eq(WarningRule::getScope, rule.getScope())
                .eq(rule.getMaterialId() != null, WarningRule::getMaterialId, rule.getMaterialId())
                .eq(rule.getWarehouseCode() != null, WarningRule::getWarehouseCode, rule.getWarehouseCode())
                .isNull(rule.getWarehouseCode() == null, WarningRule::getWarehouseCode);
        if (rule.getMaterialId() == null) wrapper.isNull(WarningRule::getMaterialId);
        if (rule.getId() != null) wrapper.ne(WarningRule::getId, rule.getId());
        Long count = ruleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalStateException("material".equals(rule.getScope())
                    ? "该物料在同仓库范围内已存在预警规则"
                    : "同仓库范围内已存在全局预警规则");
        }
    }

    private String requireName(Object value) {
        String name = text(value);
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("请填写规则名称");
        return name;
    }

    private Map<String, Object> row(WarningRule rule, Material material, Warehouse warehouse) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", rule.getId());
        result.put("name", rule.getName());
        result.put("scope", rule.getScope());
        result.put("scopeName", "material".equals(rule.getScope()) ? "指定物料" : "全局规则");
        result.put("materialId", rule.getMaterialId());
        result.put("materialSku", material == null ? null : material.getSku());
        result.put("materialName", material == null ? null : material.getName());
        result.put("materialSpec", material == null ? null : material.getSpec());
        result.put("warehouseCode", rule.getWarehouseCode());
        result.put("warehouseName", warehouse != null ? warehouse.getName() : null);
        result.put("lowThreshold", rule.getLowThreshold());
        result.put("mediumThreshold", rule.getMediumThreshold());
        result.put("highThreshold", rule.getHighThreshold());
        result.put("enabled", rule.getEnabled());
        result.put("enabledName", Integer.valueOf(1).equals(rule.getEnabled()) ? "启用" : "停用");
        result.put("remark", rule.getRemark());
        result.put("createTime", rule.getCreateTime());
        result.put("updateTime", rule.getUpdateTime());
        return result;
    }

    private Map<Long, Material> materialMap() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Warehouse> warehouseMap() {
        return warehouseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Warehouse::getCode, Function.identity(), (a, b) -> a));
    }

    private boolean contains(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long readLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        String text = text(value);
        if (!StringUtils.hasText(text)) return null;
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer readInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        String text = text(value);
        if (!StringUtils.hasText(text)) return 0;
        try {
            return new java.math.BigDecimal(text).intValue();
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Boolean readBool(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        String text = text(value);
        if (!StringUtils.hasText(text)) return null;
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }
}
