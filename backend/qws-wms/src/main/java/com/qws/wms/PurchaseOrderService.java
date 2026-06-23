package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.Material;
import com.qws.common.entity.PurchaseDemand;
import com.qws.common.entity.PurchaseOrder;
import com.qws.common.entity.PurchaseOrderItem;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.PurchaseOrderItemMapper;
import com.qws.common.mapper.PurchaseOrderMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final InboundService inboundService;

    public PurchaseOrderService(PurchaseOrderMapper orderMapper, PurchaseOrderItemMapper itemMapper,
                                MaterialMapper materialMapper, WarehouseMapper warehouseMapper,
                                InboundService inboundService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.inboundService = inboundService;
    }

    public List<Map<String, Object>> list(String keyword, String status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(PurchaseOrder::getCode, value)
                    .or().like(PurchaseOrder::getSupplier, value)
                    .or().like(PurchaseOrder::getDemandCodes, value));
        }
        if (StringUtils.hasText(status)) wrapper.eq(PurchaseOrder::getStatus, status);
        wrapper.orderByDesc(PurchaseOrder::getCreateTime).orderByDesc(PurchaseOrder::getId);
        return orderMapper.selectList(wrapper).stream().map(this::orderRow).toList();
    }

    public Map<String, Object> stats() {
        List<PurchaseOrder> rows = orderMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", rows.stream().filter(r -> "pending".equals(text(r.getStatus()))).count());
        stats.put("confirmed", rows.stream().filter(r -> "confirmed".equals(text(r.getStatus()))).count());
        stats.put("inbound", rows.stream().filter(r -> "inbound".equals(text(r.getStatus()))).count());
        stats.put("cancelled", rows.stream().filter(r -> "cancelled".equals(text(r.getStatus()))).count());
        BigDecimal totalAmount = rows.stream().map(r -> r.getTotalAmount() == null ? BigDecimal.ZERO : r.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalAmount", totalAmount);
        return stats;
    }

    public Map<String, Object> detail(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("采购订单不存在");
        Map<String, Object> result = orderRow(order);
        List<Map<String, Object>> items = itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, id)
                .orderByAsc(PurchaseOrderItem::getId)).stream().map(this::itemRow).toList();
        result.put("items", items);
        return result;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(Map<String, Object> request) {
        String supplier = text(request.get("supplier"));
        if (!StringUtils.hasText(supplier)) throw new IllegalArgumentException("请选择供应商");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("请添加物料明细");

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            String sku = text(item.get("sku"));
            String name = text(item.get("name"));
            BigDecimal qty = decimal(item.get("qty"));
            if (!StringUtils.hasText(sku) || !StringUtils.hasText(name)) throw new IllegalArgumentException("请完善物料明细的SKU与名称");
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("物料数量必须大于0");
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(qty.multiply(decimal(item.get("unitPrice"))));
        }

        LocalDateTime now = LocalDateTime.now();
        PurchaseOrder order = new PurchaseOrder();
        order.setCode(nextPoCode());
        order.setSupplier(supplier);
        order.setDemandCodes(joinDemandCodes(request.get("demandCodes")));
        order.setWarehouseCode(text(request.get("warehouseCode")));
        order.setMaterialCount(items.size());
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setDeliveryDate(parseDate(request.get("deliveryDate")));
        order.setStatus("pending");
        order.setCreator(defaultText(request.get("creator"), "admin"));
        order.setRemark(text(request.get("remark")));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        for (Map<String, Object> item : items) {
            insertItem(order.getId(), resolveMaterialId(text(item.get("sku"))), text(item.get("sku")), text(item.get("name")),
                    text(item.get("spec")), decimal(item.get("qty")), decimal(item.get("unitPrice")), now);
        }
        return detail(order.getId());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> update(Map<String, Object> request) {
        Object idValue = request.get("id");
        if (!(idValue instanceof Number number)) throw new IllegalArgumentException("缺少采购订单ID");
        Long orderId = number.longValue();
        PurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("采购订单不存在");
        if (!"pending".equals(text(order.getStatus()))) throw new IllegalStateException("仅待确认订单可修改");
        String supplier = text(request.get("supplier"));
        if (!StringUtils.hasText(supplier)) throw new IllegalArgumentException("请选择供应商");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("请添加物料明细");

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            BigDecimal qty = decimal(item.get("qty"));
            if (!StringUtils.hasText(text(item.get("sku"))) || !StringUtils.hasText(text(item.get("name")))) throw new IllegalArgumentException("请完善物料明细的SKU与名称");
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("物料数量必须大于0");
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(qty.multiply(decimal(item.get("unitPrice"))));
        }

        LocalDateTime now = LocalDateTime.now();
        order.setSupplier(supplier);
        order.setDemandCodes(joinDemandCodes(request.get("demandCodes")));
        order.setWarehouseCode(text(request.get("warehouseCode")));
        order.setMaterialCount(items.size());
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setDeliveryDate(parseDate(request.get("deliveryDate")));
        order.setRemark(text(request.get("remark")));
        order.setUpdateTime(now);
        orderMapper.updateById(order);

        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId));
        for (Map<String, Object> item : items) {
            insertItem(orderId, resolveMaterialId(text(item.get("sku"))), text(item.get("sku")), text(item.get("name")),
                    text(item.get("spec")), decimal(item.get("qty")), decimal(item.get("unitPrice")), now);
        }
        return detail(orderId);
    }

    @Transactional
    public Map<String, Object> confirm(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("采购订单不存在");
        if (!"pending".equals(text(order.getStatus()))) throw new IllegalStateException("仅待确认订单可确认");
        order.setStatus("confirmed");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancel(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("采购订单不存在");
        String status = text(order.getStatus());
        if ("inbound".equals(status)) throw new IllegalStateException("已入库订单不可取消");
        if ("cancelled".equals(status)) throw new IllegalStateException("订单已取消");
        order.setStatus("cancelled");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return detail(id);
    }

    /**
     * 为已确认的采购订单生成入库单：调用入库服务真实落库，回填入库单号并将订单转入库状态。
     */
    @Transactional
    public Map<String, Object> createInbound(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("采购订单不存在");
        if (!"confirmed".equals(text(order.getStatus()))) throw new IllegalStateException("仅已确认订单可生成入库单");

        String warehouseCode = text(order.getWarehouseCode());
        if (!StringUtils.hasText(warehouseCode)) warehouseCode = defaultWarehouse();
        String inboundCode = "IN-" + text(order.getCode());

        List<PurchaseOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, id));
        List<Map<String, Object>> inboundItems = new ArrayList<>();
        for (PurchaseOrderItem item : items) {
            Map<String, Object> inboundItem = new HashMap<>();
            inboundItem.put("sku", item.getSku());
            inboundItem.put("name", item.getMaterialName());
            inboundItem.put("spec", item.getSpec());
            inboundItem.put("method", "qty");
            inboundItem.put("qty", item.getQty());
            inboundItem.put("unitPrice", item.getUnitPrice());
            inboundItems.add(inboundItem);
        }

        Map<String, Object> inboundRequest = new HashMap<>();
        inboundRequest.put("code", inboundCode);
        inboundRequest.put("poCode", text(order.getCode()));
        inboundRequest.put("supplier", text(order.getSupplier()));
        inboundRequest.put("warehouseCode", warehouseCode);
        inboundRequest.put("creator", defaultText(order.getCreator(), "admin"));
        inboundRequest.put("remark", "采购订单" + text(order.getCode()) + "生成");
        inboundRequest.put("items", inboundItems);
        inboundService.create(inboundRequest);

        order.setStatus("inbound");
        order.setInboundCode(inboundCode);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    /**
     * 由选中的采购需求合并生成一张采购订单（含明细），返回订单号与订单ID。
     * 供 PurchaseDemandService.generatePO 调用，确保"生成采购订单"真正落库到 wms_purchase_order。
     */
    @Transactional
    public Map<String, Object> createFromDemands(List<PurchaseDemand> demands, String creator) {
        if (demands == null || demands.isEmpty()) throw new IllegalArgumentException("没有可生成订单的采购需求");
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        String warehouseCode = "";
        List<String> demandCodes = new ArrayList<>();
        for (PurchaseDemand d : demands) {
            BigDecimal qty = d.getQty() == null ? BigDecimal.ZERO : d.getQty();
            BigDecimal price = d.getEstimatedPrice() == null ? BigDecimal.ZERO : d.getEstimatedPrice();
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(qty.multiply(price));
            if (!StringUtils.hasText(warehouseCode)) warehouseCode = text(d.getWarehouseCode());
            String dcode = text(d.getCode());
            if (StringUtils.hasText(dcode)) demandCodes.add(dcode);
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setCode(nextPoCode());
        order.setSupplier("待指定供应商");
        order.setDemandCodes(String.join(",", demandCodes));
        order.setWarehouseCode(warehouseCode);
        order.setMaterialCount(demands.size());
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setDeliveryDate(LocalDate.now().plusDays(7));
        order.setStatus("pending");
        order.setCreator(defaultText(creator, "admin"));
        order.setRemark("采购需求生成");
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        for (PurchaseDemand d : demands) {
            insertItem(order.getId(), d.getMaterialId(), text(d.getSku()), text(d.getMaterialName()),
                    text(d.getSpec()), d.getQty() == null ? BigDecimal.ZERO : d.getQty(),
                    d.getEstimatedPrice() == null ? BigDecimal.ZERO : d.getEstimatedPrice(), now);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("poCode", order.getCode());
        result.put("orderId", order.getId());
        return result;
    }

    private void insertItem(Long orderId, Long materialId, String sku, String name, String spec,
                            BigDecimal qty, BigDecimal unitPrice, LocalDateTime now) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setOrderId(orderId);
        item.setMaterialId(materialId);
        item.setSku(sku);
        item.setMaterialName(name);
        item.setSpec(spec);
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        item.setCreateTime(now);
        itemMapper.insert(item);
    }

    private Long resolveMaterialId(String sku) {
        if (!StringUtils.hasText(sku)) return null;
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getSku, sku).last("LIMIT 1"));
        return material == null ? null : material.getId();
    }

    private String defaultWarehouse() {
        Warehouse warehouse = warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>()
                .orderByAsc(Warehouse::getId).last("LIMIT 1"));
        return warehouse == null ? "WH-A" : warehouse.getCode();
    }

    private String nextPoCode() {
        String prefix = "PO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + String.format("%03d", nextSeq(prefix));
    }

    /**
     * 取已有编号的最大流水号 +1，避免删除记录后 COUNT 与现存编号冲突触发唯一约束。
     */
    private int nextSeq(String prefix) {
        List<PurchaseOrder> rows = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .likeRight(PurchaseOrder::getCode, prefix)
                .select(PurchaseOrder::getCode));
        int max = 0;
        for (PurchaseOrder row : rows) {
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

    private String joinDemandCodes(Object value) {
        if (value instanceof List<?> list) {
            return String.join(",", list.stream().map(String::valueOf).filter(StringUtils::hasText).toList());
        }
        return text(value);
    }

    private Map<String, Object> orderRow(PurchaseOrder source) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", source.getId());
        result.put("code", source.getCode());
        result.put("supplier", source.getSupplier());
        String demandCodes = text(source.getDemandCodes());
        result.put("demandCodes", StringUtils.hasText(demandCodes)
                ? Arrays.stream(demandCodes.split(",")).filter(StringUtils::hasText).toList() : new ArrayList<>());
        result.put("warehouseCode", source.getWarehouseCode());
        result.put("materialCount", source.getMaterialCount());
        result.put("totalQty", source.getTotalQty());
        result.put("totalAmount", source.getTotalAmount());
        result.put("deliveryDate", source.getDeliveryDate());
        result.put("status", source.getStatus());
        result.put("inboundCode", source.getInboundCode());
        result.put("creator", source.getCreator());
        result.put("remark", source.getRemark());
        result.put("createTime", source.getCreateTime());
        return result;
    }

    private Map<String, Object> itemRow(PurchaseOrderItem source) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", source.getId());
        result.put("materialId", source.getMaterialId());
        result.put("sku", source.getSku());
        result.put("name", source.getMaterialName());
        result.put("spec", source.getSpec());
        result.put("qty", source.getQty());
        result.put("unitPrice", source.getUnitPrice());
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
}
