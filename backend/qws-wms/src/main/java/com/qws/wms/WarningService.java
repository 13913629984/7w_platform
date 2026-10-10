package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.Inventory;
import com.qws.common.entity.Material;
import com.qws.common.entity.StockWarning;
import com.qws.common.entity.Warehouse;
import com.qws.common.entity.WarningRule;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.StockWarningMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WarningService {

    private static final List<String> LEVEL_ORDER = List.of("high", "medium", "low");

    private final StockWarningMapper warningMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final WarningRuleService warningRuleService;

    public WarningService(StockWarningMapper warningMapper, InventoryMapper inventoryMapper,
                          MaterialMapper materialMapper, WarehouseMapper warehouseMapper,
                          WarningRuleService warningRuleService) {
        this.warningMapper = warningMapper;
        this.inventoryMapper = inventoryMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.warningRuleService = warningRuleService;
    }

    public List<Map<String, Object>> list(String keyword, String level, String status) {
        LambdaQueryWrapper<StockWarning> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(level)) wrapper.eq(StockWarning::getLevel, level);
        if (StringUtils.hasText(status)) wrapper.eq(StockWarning::getStatus, status);
        List<StockWarning> warnings = warningMapper.selectList(wrapper);

        Map<Long, Material> materialMap = materialMap();
        Map<String, Warehouse> warehouseMap = warehouseMap();

        List<Map<String, Object>> rows = warnings.stream()
                .map(w -> row(w, materialMap.get(w.getMaterialId()), warehouseMap.get(w.getWarehouseCode())))
                .collect(Collectors.toList());

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream().filter(r -> contains(r.get("sku"), kw) || contains(r.get("name"), kw))
                    .collect(Collectors.toList());
        }

        rows.sort(Comparator
                .comparingInt((Map<String, Object> r) -> levelRank(text(r.get("level"))))
                .thenComparing(r -> warningTimeKey(r.get("warningTime")), Comparator.reverseOrder())
                .thenComparing(r -> idKey(r.get("id")), Comparator.reverseOrder()));
        return rows;
    }

    public Map<String, Object> stats() {
        List<StockWarning> rows = warningMapper.selectList(null);
        long total = rows.size();
        long handled = rows.stream().filter(r -> "handled".equals(text(r.getStatus()))).count();
        long pending = rows.stream().filter(r -> "pending".equals(text(r.getStatus()))).count();
        long high = rows.stream().filter(r -> "high".equals(text(r.getLevel()))).count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("handled", handled);
        stats.put("pending", pending);
        stats.put("high", high);
        return stats;
    }

    @Transactional
    public Map<String, Object> scan() {
        LocalDateTime now = LocalDateTime.now();
        List<Aggregate> aggregates = aggregateInventory();
        List<WarningRule> rules = warningRuleService.enabledRules();

        int created = 0;
        int updated = 0;
        int resolved = 0;
        List<Long> activeWarningIds = new ArrayList<>();

        for (Aggregate agg : aggregates) {
            // 优先命中物料级 / 全局预警规则，未命中时回退到库存安全库存
            WarningRule rule = warningRuleService.matchRule(rules, agg.materialId, agg.warehouseCode);
            Thresholds thresholds = thresholdsOf(agg.safeStock, rule);
            String level = calcLevel(agg.totalQty, thresholds);
            if (level == null) continue;

            StockWarning existing = warningMapper.selectOne(new LambdaQueryWrapper<StockWarning>()
                    .eq(StockWarning::getMaterialId, agg.materialId)
                    .eq(StockWarning::getWarehouseCode, agg.warehouseCode));
            if (existing == null) {
                StockWarning warning = new StockWarning();
                warning.setMaterialId(agg.materialId);
                warning.setWarehouseCode(agg.warehouseCode);
                warning.setCurrentQty(agg.totalQty);
                warning.setThreshold(thresholds.low());
                warning.setRuleId(rule == null ? null : rule.getId());
                warning.setRuleName(rule == null ? null : rule.getName());
                warning.setLevel(level);
                warning.setStatus("pending");
                warning.setWarningTime(now);
                warning.setCreateTime(now);
                warning.setUpdateTime(now);
                warningMapper.insert(warning);
                created++;
                activeWarningIds.add(warning.getId());
            } else {
                String currentStatus = text(existing.getStatus());
                // 库存仍处于预警区间：刷新数值/级别。已处理且仍预警的，重新置为待处理。
                String newStatus = "resolved".equals(currentStatus) ? "pending" : currentStatus;
                existing.setCurrentQty(agg.totalQty);
                existing.setThreshold(thresholds.low());
                existing.setRuleId(rule == null ? null : rule.getId());
                existing.setRuleName(rule == null ? null : rule.getName());
                existing.setLevel(level);
                existing.setStatus(newStatus);
                existing.setWarningTime(now);
                existing.setUpdateTime(now);
                warningMapper.updateById(existing);
                updated++;
                activeWarningIds.add(existing.getId());
            }
        }

        // 库存已恢复正常但仍存在未关闭预警的，自动标记为已解除
        List<StockWarning> openWarnings = warningMapper.selectList(new LambdaQueryWrapper<StockWarning>()
                .ne(StockWarning::getStatus, "resolved"));
        for (StockWarning warning : openWarnings) {
            if (!activeWarningIds.contains(warning.getId())) {
                warning.setStatus("resolved");
                warning.setHandleRemark("库存已恢复正常");
                warning.setHandleTime(now);
                warning.setUpdateTime(now);
                warningMapper.updateById(warning);
                resolved++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("resolved", resolved);
        result.put("scanned", aggregates.size());
        return result;
    }

    @Transactional
    public Map<String, Object> handle(Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) throw new IllegalArgumentException("缺少预警ID");
        Long warningId = number.longValue();
        String handler = text(request.get("handler"));
        if (!StringUtils.hasText(handler)) throw new IllegalArgumentException("请填写处理人");
        StockWarning warning = warningMapper.selectById(warningId);
        if (warning == null) throw new IllegalArgumentException("预警不存在");
        LocalDateTime now = LocalDateTime.now();
        warning.setStatus("handled");
        warning.setHandler(handler);
        warning.setHandleRemark(text(request.get("remark")));
        warning.setHandleTime(now);
        warning.setUpdateTime(now);
        warningMapper.updateById(warning);
        return detail(warningId);
    }

    public Map<String, Object> detail(Long id) {
        StockWarning warning = warningMapper.selectById(id);
        if (warning == null) throw new IllegalArgumentException("预警不存在");
        Material material = warning.getMaterialId() == null ? null : materialMapper.selectById(warning.getMaterialId());
        Warehouse warehouse = warehouseMap().get(warning.getWarehouseCode());
        return row(warning, material, warehouse);
    }

    @Transactional
    public void delete(Long id) {
        warningMapper.deleteById(id);
    }

    private List<Aggregate> aggregateInventory() {
        Map<Long, Material> materialMap = materialMap();
        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getStatus, 1));
        Map<String, Aggregate> grouped = new LinkedHashMap<>();
        for (Inventory inv : inventories) {
            if (inv.getMaterialId() == null) continue;
            String key = inv.getMaterialId() + "::" + text(inv.getWarehouseCode());
            Aggregate agg = grouped.computeIfAbsent(key, k -> {
                Aggregate a = new Aggregate();
                a.materialId = inv.getMaterialId();
                a.warehouseCode = text(inv.getWarehouseCode());
                a.totalQty = BigDecimal.ZERO;
                a.safeStock = 0;
                return a;
            });
            agg.totalQty = agg.totalQty.add(inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity());
            int safe = coalesceSafeStock(inv, materialMap.get(inv.getMaterialId()));
            if (safe > agg.safeStock) agg.safeStock = safe;
        }
        return new ArrayList<>(grouped.values());
    }

    private int coalesceSafeStock(Inventory inv, Material material) {
        if (inv.getSafeStock() != null) return inv.getSafeStock();
        if (material != null && material.getSafeStock() != null) return material.getSafeStock();
        return 0;
    }

    private Map<Long, Material> materialMap() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Warehouse> warehouseMap() {
        return warehouseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Warehouse::getCode, Function.identity(), (a, b) -> a));
    }

    /** 命中规则时使用规则阈值，否则按安全库存推导（高危=0，中危=安全库存/2，低危=安全库存）。 */
    private Thresholds thresholdsOf(int safeStock, WarningRule rule) {
        if (rule != null) {
            return new Thresholds(nvl(rule.getLowThreshold()), nvl(rule.getMediumThreshold()), nvl(rule.getHighThreshold()));
        }
        return new Thresholds(safeStock, safeStock / 2, 0);
    }

    private String calcLevel(BigDecimal totalQty, Thresholds thresholds) {
        if (totalQty == null) return null;
        if (totalQty.signum() <= 0) return "high";
        if (thresholds.high() > 0 && totalQty.compareTo(BigDecimal.valueOf(thresholds.high())) <= 0) return "high";
        if (thresholds.medium() > 0 && totalQty.compareTo(BigDecimal.valueOf(thresholds.medium())) <= 0) return "medium";
        if (thresholds.low() > 0 && totalQty.compareTo(BigDecimal.valueOf(thresholds.low())) <= 0) return "low";
        return null;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private Map<String, Object> row(StockWarning warning, Material material, Warehouse warehouse) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", warning.getId());
        result.put("materialId", warning.getMaterialId());
        result.put("sku", material == null ? null : material.getSku());
        result.put("name", material == null ? null : material.getName());
        result.put("spec", material == null ? null : material.getSpec());
        result.put("unit", material == null ? null : material.getUnit());
        result.put("warehouseCode", warning.getWarehouseCode());
        result.put("warehouse", warehouse != null ? warehouse.getName() : warning.getWarehouseCode());
        result.put("currentQty", warning.getCurrentQty());
        result.put("threshold", warning.getThreshold());
        result.put("ruleId", warning.getRuleId());
        result.put("ruleName", warning.getRuleName());
        result.put("level", warning.getLevel());
        result.put("levelName", levelName(text(warning.getLevel())));
        result.put("status", warning.getStatus());
        result.put("statusName", statusName(text(warning.getStatus())));
        result.put("handler", warning.getHandler());
        result.put("handleRemark", warning.getHandleRemark());
        result.put("handleTime", warning.getHandleTime());
        result.put("warningTime", warning.getWarningTime());
        return result;
    }

    private int levelRank(String level) {
        int idx = LEVEL_ORDER.indexOf(level);
        return idx < 0 ? LEVEL_ORDER.size() : idx;
    }

    private String warningTimeKey(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long idKey(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private String levelName(String level) {
        return switch (level) {
            case "high" -> "高危";
            case "medium" -> "中危";
            case "low" -> "低危";
            default -> level;
        };
    }

    private String statusName(String status) {
        return switch (status) {
            case "pending" -> "待处理";
            case "handled" -> "已处理";
            case "resolved" -> "已解除";
            default -> status;
        };
    }

    private boolean contains(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static class Aggregate {
        Long materialId;
        String warehouseCode;
        BigDecimal totalQty;
        int safeStock;
    }

    /** 预警阈值区间：high ≤ medium ≤ low。 */
    private record Thresholds(int low, int medium, int high) {}
}
