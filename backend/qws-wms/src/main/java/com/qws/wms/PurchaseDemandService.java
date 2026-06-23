package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.Inventory;
import com.qws.common.entity.Material;
import com.qws.common.entity.PurchaseDemand;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.PurchaseDemandMapper;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseDemandService {

    private final PurchaseDemandMapper demandMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseDemandService(PurchaseDemandMapper demandMapper, InventoryMapper inventoryMapper,
                                 MaterialMapper materialMapper, PurchaseOrderService purchaseOrderService) {
        this.demandMapper = demandMapper;
        this.inventoryMapper = inventoryMapper;
        this.materialMapper = materialMapper;
        this.purchaseOrderService = purchaseOrderService;
    }

    public List<Map<String, Object>> list(String keyword, String source, String status) {
        LambdaQueryWrapper<PurchaseDemand> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(PurchaseDemand::getCode, value)
                    .or().like(PurchaseDemand::getSku, value)
                    .or().like(PurchaseDemand::getMaterialName, value));
        }
        if (StringUtils.hasText(source)) wrapper.eq(PurchaseDemand::getSource, source);
        if (StringUtils.hasText(status)) wrapper.eq(PurchaseDemand::getStatus, status);
        wrapper.orderByDesc(PurchaseDemand::getCreateTime).orderByDesc(PurchaseDemand::getId);
        return demandMapper.selectList(wrapper).stream().map(this::row).toList();
    }

    public Map<String, Object> stats() {
        List<PurchaseDemand> rows = demandMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", rows.stream().filter(r -> "pending".equals(text(r.getStatus()))).count());
        stats.put("ordered", rows.stream().filter(r -> "ordered".equals(text(r.getStatus()))).count());
        stats.put("completed", rows.stream().filter(r -> "completed".equals(text(r.getStatus()))).count());
        stats.put("triggerCount", (long) triggers().size());
        return stats;
    }

    public Map<String, Object> detail(Long id) {
        PurchaseDemand demand = demandMapper.selectById(id);
        if (demand == null) throw new IllegalArgumentException("采购需求不存在");
        return row(demand);
    }

    /**
     * 库存预警触发的采购需求：按物料+仓库聚合库存，低于安全库存且尚未有待处理需求的，给出建议采购量。
     */
    public List<Map<String, Object>> triggers() {
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

        List<Map<String, Object>> result = new ArrayList<>();
        for (Aggregate agg : grouped.values()) {
            if (agg.safeStock <= 0 || agg.totalQty.compareTo(BigDecimal.valueOf(agg.safeStock)) >= 0) continue;
            // 已存在该物料+仓库的待处理/已下单需求则跳过，避免重复
            Long exists = demandMapper.selectCount(new LambdaQueryWrapper<PurchaseDemand>()
                    .eq(PurchaseDemand::getMaterialId, agg.materialId)
                    .eq(PurchaseDemand::getWarehouseCode, agg.warehouseCode)
                    .in(PurchaseDemand::getStatus, List.of("pending", "ordered")));
            if (exists != null && exists > 0) continue;
            // 建议采购量：补足到安全库存的 1.5 倍
            BigDecimal suggestQty = BigDecimal.valueOf(agg.safeStock).multiply(BigDecimal.valueOf(1.5))
                    .subtract(agg.totalQty).setScale(0, java.math.RoundingMode.CEILING);
            Material material = materialMap.get(agg.materialId);
            Map<String, Object> item = new HashMap<>();
            item.put("materialId", agg.materialId);
            item.put("warehouseCode", agg.warehouseCode);
            item.put("sku", material == null ? null : material.getSku());
            item.put("name", material == null ? null : material.getName());
            item.put("spec", material == null ? null : material.getSpec());
            item.put("currentStock", agg.totalQty);
            item.put("safetyStock", agg.safeStock);
            item.put("warningStock", agg.safeStock);
            item.put("suggestQty", suggestQty);
            item.put("estimatedPrice", material == null ? null : material.getUnitPrice());
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String sku = text(request.get("sku"));
        String name = text(request.get("materialName"));
        BigDecimal qty = decimal(request.get("qty"));
        if (!StringUtils.hasText(sku)) throw new IllegalArgumentException("请填写物料编码");
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("请填写物料名称");
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("需求数量必须大于0");
        LocalDateTime now = LocalDateTime.now();
        PurchaseDemand demand = new PurchaseDemand();
        demand.setCode(nextDemandCode());
        demand.setSource(defaultText(request.get("source"), "manual"));
        demand.setMaterialId(resolveMaterialId(sku));
        demand.setSku(sku);
        demand.setMaterialName(name);
        demand.setSpec(text(request.get("spec")));
        demand.setWarehouseCode(text(request.get("warehouseCode")));
        demand.setQty(qty);
        demand.setEstimatedPrice(decimal(request.get("estimatedPrice")));
        demand.setDemandDate(parseDate(request.get("demandDate")));
        demand.setStatus("pending");
        demand.setRequester(defaultText(request.get("requester"), "admin"));
        demand.setRemark(text(request.get("remark")));
        demand.setCreateTime(now);
        demand.setUpdateTime(now);
        demandMapper.insert(demand);
        return detail(demand.getId());
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> request) {
        Object idValue = request.get("id");
        if (!(idValue instanceof Number number)) throw new IllegalArgumentException("缺少需求ID");
        Long id = number.longValue();
        PurchaseDemand demand = demandMapper.selectById(id);
        if (demand == null) throw new IllegalArgumentException("采购需求不存在");
        if (!"pending".equals(text(demand.getStatus()))) throw new IllegalStateException("仅待处理需求可修改");
        String sku = text(request.get("sku"));
        String name = text(request.get("materialName"));
        BigDecimal qty = decimal(request.get("qty"));
        if (!StringUtils.hasText(sku)) throw new IllegalArgumentException("请填写物料编码");
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("请填写物料名称");
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("需求数量必须大于0");
        demand.setSource(defaultText(request.get("source"), text(demand.getSource())));
        demand.setMaterialId(resolveMaterialId(sku));
        demand.setSku(sku);
        demand.setMaterialName(name);
        demand.setSpec(text(request.get("spec")));
        demand.setWarehouseCode(text(request.get("warehouseCode")));
        demand.setQty(qty);
        demand.setEstimatedPrice(decimal(request.get("estimatedPrice")));
        demand.setDemandDate(parseDate(request.get("demandDate")));
        demand.setRemark(text(request.get("remark")));
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.updateById(demand);
        return detail(id);
    }

    /**
     * 从库存预警触发项批量生成采购需求。items 为空时生成当前全部触发项。
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> createFromTriggers(Map<String, Object> request) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        List<Map<String, Object>> triggers = (items == null || items.isEmpty()) ? triggers() : items;
        if (triggers.isEmpty()) throw new IllegalArgumentException("当前没有可生成的预警触发需求");
        String requester = defaultText(request.get("requester"), "admin");
        int created = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> trigger : triggers) {
            String sku = text(trigger.get("sku"));
            if (!StringUtils.hasText(sku)) continue;
            Long materialId = trigger.get("materialId") instanceof Number n ? n.longValue() : resolveMaterialId(sku);
            PurchaseDemand demand = new PurchaseDemand();
            demand.setCode(nextDemandCode());
            demand.setSource("warning");
            demand.setMaterialId(materialId);
            demand.setSku(sku);
            demand.setMaterialName(text(trigger.get("name")));
            demand.setSpec(text(trigger.get("spec")));
            demand.setWarehouseCode(text(trigger.get("warehouseCode")));
            demand.setQty(decimal(trigger.get("suggestQty")));
            demand.setEstimatedPrice(decimal(trigger.get("estimatedPrice")));
            demand.setDemandDate(LocalDate.now().plusDays(7));
            demand.setStatus("pending");
            demand.setRequester(requester);
            demand.setRemark("库存预警触发自动生成");
            demand.setCreateTime(now);
            demand.setUpdateTime(now);
            demandMapper.insert(demand);
            created++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        return result;
    }

    /**
     * MRP 计算：基于库存预警触发逻辑生成待处理需求（等价于全量生成触发项）。
     */
    @Transactional
    public Map<String, Object> mrp() {
        return createFromTriggers(new HashMap<>());
    }

    /**
     * 将选中的待处理需求合并生成一张采购订单：真正在 wms_purchase_order 落库（含明细），
     * 再回填这些需求的状态与订单号。
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> generatePO(Map<String, Object> request) {
        List<Object> ids = (List<Object>) request.get("ids");
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("请选择待处理采购需求");
        // 查出选中且仍为待处理的需求
        List<PurchaseDemand> demands = new ArrayList<>();
        for (Object idValue : ids) {
            if (!(idValue instanceof Number number)) continue;
            PurchaseDemand demand = demandMapper.selectById(number.longValue());
            if (demand != null && "pending".equals(text(demand.getStatus()))) demands.add(demand);
        }
        if (demands.isEmpty()) throw new IllegalStateException("选中的需求均非待处理状态");

        // 真正生成采购订单（含明细）
        Map<String, Object> order = purchaseOrderService.createFromDemands(demands, defaultText(request.get("creator"), "admin"));
        String poCode = text(order.get("poCode"));

        // 回填需求状态与订单号
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;
        for (PurchaseDemand demand : demands) {
            demand.setStatus("ordered");
            demand.setPoCode(poCode);
            demand.setUpdateTime(now);
            updated += demandMapper.updateById(demand);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("poCode", poCode);
        result.put("orderId", order.get("orderId"));
        result.put("updated", updated);
        return result;
    }

    @Transactional
    public void delete(Long id) {
        demandMapper.deleteById(id);
    }

    private Long resolveMaterialId(String sku) {
        if (!StringUtils.hasText(sku)) return null;
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getSku, sku).last("LIMIT 1"));
        return material == null ? null : material.getId();
    }

    private String nextDemandCode() {
        String prefix = "PR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + String.format("%03d", nextSeq(prefix) );
    }

    /**
     * 取已有编号的最大流水号 +1，避免删除记录后 COUNT 与现存编号冲突触发唯一约束。
     */
    private int nextSeq(String prefix) {
        List<PurchaseDemand> rows = demandMapper.selectList(new LambdaQueryWrapper<PurchaseDemand>()
                .likeRight(PurchaseDemand::getCode, prefix)
                .select(PurchaseDemand::getCode));
        int max = 0;
        for (PurchaseDemand row : rows) {
            String code = text(row.getCode());
            if (code.length() <= prefix.length()) continue;
            try {
                max = Math.max(max, Integer.parseInt(code.substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // 编号尾部非纯数字时忽略
            }
        }
        return max + 1;
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

    private Map<String, Object> row(PurchaseDemand source) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", source.getId());
        result.put("code", source.getCode());
        result.put("source", source.getSource());
        result.put("materialId", source.getMaterialId());
        result.put("sku", source.getSku());
        result.put("materialName", source.getMaterialName());
        result.put("spec", source.getSpec());
        result.put("warehouseCode", source.getWarehouseCode());
        result.put("qty", source.getQty());
        result.put("estimatedPrice", source.getEstimatedPrice());
        result.put("demandDate", source.getDemandDate());
        result.put("status", source.getStatus());
        result.put("poCode", source.getPoCode());
        result.put("requester", source.getRequester());
        result.put("remark", source.getRemark());
        result.put("createTime", source.getCreateTime());
        return result;
    }

    private LocalDate parseDate(Object value) {
        String text = text(value);
        if (!StringUtils.hasText(text)) return LocalDate.now().plusDays(7);
        try { return LocalDate.parse(text.substring(0, 10)); }
        catch (Exception ex) { return LocalDate.now().plusDays(7); }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String defaultText(Object value, String defaultValue) {
        String text = text(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private BigDecimal decimal(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return BigDecimal.ZERO;
        return new BigDecimal(String.valueOf(value));
    }

    private static class Aggregate {
        Long materialId;
        String warehouseCode;
        BigDecimal totalQty;
        int safeStock;
    }
}
