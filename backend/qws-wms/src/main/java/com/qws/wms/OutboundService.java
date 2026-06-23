package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.InboundOrder;
import com.qws.common.entity.InboundSn;
import com.qws.common.entity.Inventory;
import com.qws.common.entity.Location;
import com.qws.common.entity.Material;
import com.qws.common.entity.OutboundItem;
import com.qws.common.entity.OutboundOrder;
import com.qws.common.entity.OutboundRecord;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.InboundOrderMapper;
import com.qws.common.mapper.InboundSnMapper;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.LocationMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.OutboundItemMapper;
import com.qws.common.mapper.OutboundOrderMapper;
import com.qws.common.mapper.OutboundRecordMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OutboundService {

    private final OutboundOrderMapper orderMapper;
    private final OutboundItemMapper itemMapper;
    private final OutboundRecordMapper recordMapper;
    private final InboundSnMapper snMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;

    public OutboundService(OutboundOrderMapper orderMapper, OutboundItemMapper itemMapper, OutboundRecordMapper recordMapper,
                           InboundSnMapper snMapper, InboundOrderMapper inboundOrderMapper, InventoryMapper inventoryMapper,
                           MaterialMapper materialMapper, WarehouseMapper warehouseMapper, LocationMapper locationMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.recordMapper = recordMapper;
        this.snMapper = snMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.inventoryMapper = inventoryMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.locationMapper = locationMapper;
    }

    public List<Map<String, Object>> list(String keyword, String status) {
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(OutboundOrder::getCode, value)
                    .or().like(OutboundOrder::getOrderNo, value)
                    .or().like(OutboundOrder::getCustomer, value));
        }
        if (StringUtils.hasText(status)) wrapper.eq(OutboundOrder::getStatus, status);
        wrapper.orderByDesc(OutboundOrder::getCreateTime).orderByDesc(OutboundOrder::getId);
        Map<String, Warehouse> warehouseMap = warehouseMap();
        return orderMapper.selectList(wrapper).stream().map(o -> orderRow(o, warehouseMap)).toList();
    }

    public List<Map<String, Object>> details(String keyword, String method) {
        LambdaQueryWrapper<OutboundRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(method)) wrapper.eq(OutboundRecord::getMethod, method);
        wrapper.orderByDesc(OutboundRecord::getCreateTime).orderByDesc(OutboundRecord::getId);
        List<OutboundRecord> records = recordMapper.selectList(wrapper);

        Map<Long, Material> materialMap = materialMap();
        Map<String, Location> locationMap = locationMap();
        Map<Long, OutboundOrder> orderMap = orderMap();

        List<Map<String, Object>> rows = records.stream()
                .map(r -> recordRow(r, orderMap.get(r.getOrderId()), materialMap.get(r.getMaterialId()), locationMap.get(r.getLocationCode())))
                .collect(Collectors.toList());
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream().filter(r -> contains(r.get("outboundCode"), kw)
                    || contains(r.get("sku"), kw) || contains(r.get("name"), kw)).collect(Collectors.toList());
        }
        return rows;
    }

    public List<Map<String, Object>> snList(String keyword, String status) {
        LambdaQueryWrapper<InboundSn> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) wrapper.eq(InboundSn::getStatus, status);
        wrapper.orderByDesc(InboundSn::getCreateTime).orderByDesc(InboundSn::getId);
        List<InboundSn> sns = snMapper.selectList(wrapper);

        Map<Long, Material> materialMap = materialMap();
        Map<String, Location> locationMap = locationMap();
        Map<Long, InboundOrder> inboundOrderMap = inboundOrderMap();

        List<Map<String, Object>> rows = sns.stream()
                .map(s -> snRow(s, inboundOrderMap.get(s.getOrderId()), materialMap.get(s.getMaterialId()), locationMap.get(s.getLocationCode())))
                .collect(Collectors.toList());
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream().filter(r -> contains(r.get("sn"), kw) || contains(r.get("sku"), kw)
                    || contains(r.get("name"), kw) || contains(r.get("outboundCode"), kw)).collect(Collectors.toList());
        }
        return rows;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(Map<String, Object> request) {
        String code = text(request.get("code"));
        String customer = text(request.get("customer"));
        String warehouseCode = text(request.get("warehouseCode"));
        String creator = text(request.get("creator"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        if (!StringUtils.hasText(code)) throw new IllegalArgumentException("请填写出库单号");
        if (!StringUtils.hasText(customer)) throw new IllegalArgumentException("请填写客户名称");
        if (!StringUtils.hasText(warehouseCode)) throw new IllegalArgumentException("请选择出库仓库");
        if (!StringUtils.hasText(creator)) throw new IllegalArgumentException("请填写出库人");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("请添加物料明细");
        if (orderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getCode, code)) > 0) {
            throw new IllegalStateException("出库单号已存在");
        }

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            BigDecimal qty = decimal(item.get("qty"));
            BigDecimal unitPrice = decimal(item.get("unitPrice"));
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("物料数量必须大于0");
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(qty.multiply(unitPrice));
        }

        LocalDateTime now = LocalDateTime.now();
        OutboundOrder order = new OutboundOrder();
        order.setCode(code);
        order.setOrderNo(text(request.get("orderNo")));
        order.setCustomer(customer);
        order.setWarehouseCode(warehouseCode);
        order.setMaterialCount(items.size());
        order.setTotalQty(totalQty);
        order.setOutboundQty(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);
        order.setStatus("pending");
        order.setCreator(creator);
        order.setRemark(text(request.get("remark")));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        for (Map<String, Object> item : items) {
            Long materialId = resolveMaterialId(item);
            OutboundItem entity = new OutboundItem();
            entity.setOrderId(order.getId());
            entity.setMaterialId(materialId);
            entity.setLocationCode(text(item.get("locationCode")));
            entity.setBatchNo(text(item.get("batchNo")));
            entity.setMethod(defaultText(item.get("method"), "qty"));
            entity.setQuantity(decimal(item.get("qty")));
            entity.setOutboundQty(BigDecimal.ZERO);
            entity.setUnitPrice(decimal(item.get("unitPrice")));
            entity.setRemark(text(item.get("remark")));
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            itemMapper.insert(entity);
        }
        return detail(order.getId());
    }

    public Map<String, Object> detail(Long id) {
        OutboundOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("出库单不存在");
        Map<String, Warehouse> warehouseMap = warehouseMap();
        Map<Long, Material> materialMap = materialMap();
        Map<String, Location> locationMap = locationMap();
        Map<Long, OutboundOrder> orderMap = Map.of(order.getId(), order);

        Map<String, Object> result = orderRow(order, warehouseMap);
        List<Map<String, Object>> items = itemMapper.selectList(new LambdaQueryWrapper<OutboundItem>()
                        .eq(OutboundItem::getOrderId, id).orderByAsc(OutboundItem::getId)).stream()
                .map(i -> itemRow(i, materialMap.get(i.getMaterialId()))).toList();
        result.put("items", items);
        List<Map<String, Object>> records = recordMapper.selectList(new LambdaQueryWrapper<OutboundRecord>()
                        .eq(OutboundRecord::getOrderId, id)
                        .orderByDesc(OutboundRecord::getCreateTime).orderByDesc(OutboundRecord::getId)).stream()
                .map(r -> recordRow(r, orderMap.get(r.getOrderId()), materialMap.get(r.getMaterialId()), locationMap.get(r.getLocationCode()))).toList();
        result.put("records", records);
        return result;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) throw new IllegalArgumentException("缺少出库单ID");
        Long orderId = number.longValue();
        String operator = text(request.get("operator"));
        if (!StringUtils.hasText(operator)) throw new IllegalArgumentException("请填写出库操作员");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("请填写出库明细");

        OutboundOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("出库单不存在");
        if ("completed".equals(text(order.getStatus()))) throw new IllegalStateException("出库单已完成");
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalExecutedQty = BigDecimal.ZERO;

        for (Map<String, Object> executeItem : items) {
            Object itemIdValue = executeItem.get("id");
            if (!(itemIdValue instanceof Number itemNumber)) throw new IllegalArgumentException("缺少出库明细ID");
            Long itemId = itemNumber.longValue();
            OutboundItem item = itemMapper.selectOne(new LambdaQueryWrapper<OutboundItem>()
                    .eq(OutboundItem::getId, itemId).eq(OutboundItem::getOrderId, orderId));
            if (item == null) throw new IllegalArgumentException("出库明细不存在");
            BigDecimal quantity = nz(item.getQuantity());
            BigDecimal outboundQty = nz(item.getOutboundQty());
            BigDecimal remainingQty = quantity.subtract(outboundQty);
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) continue;
            String method = text(item.getMethod());
            List<Map<String, Object>> locations = (List<Map<String, Object>>) executeItem.get("locations");
            if (locations == null || locations.isEmpty()) throw new IllegalArgumentException("请填写出库库位");
            BigDecimal itemExecutedQty = BigDecimal.ZERO;

            for (Map<String, Object> location : locations) {
                BigDecimal qty = decimal(location.get("qty"));
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
                String locationCode = defaultText(location.get("locationCode"), text(location.get("location")));
                if (!StringUtils.hasText(locationCode)) throw new IllegalArgumentException("请选择出库库位");
                itemExecutedQty = itemExecutedQty.add(qty);
                if (itemExecutedQty.compareTo(remainingQty) > 0) throw new IllegalArgumentException("本次出库数量不能超过剩余可出库数量");
                decreaseInventory(item, order, locationCode, qty, now);

                OutboundRecord record = new OutboundRecord();
                record.setOrderId(orderId);
                record.setItemId(itemId);
                record.setMaterialId(item.getMaterialId());
                record.setLocationCode(locationCode);
                record.setBatchNo(item.getBatchNo());
                record.setMethod(method);
                record.setQuantity(qty);
                record.setUnitPrice(item.getUnitPrice());
                record.setOperator(operator);
                record.setCreateTime(now);
                recordMapper.insert(record);

                if ("sn".equals(method)) {
                    List<String> sns = parseSnList(location.get("snList"));
                    if (sns.size() != qty.intValue()) throw new IllegalArgumentException("SN码数量与出库数量不一致");
                    for (String sn : sns) {
                        InboundSn snEntity = snMapper.selectOne(new LambdaQueryWrapper<InboundSn>()
                                .eq(InboundSn::getSn, sn).eq(InboundSn::getStatus, "inbound").eq(InboundSn::getMaterialId, item.getMaterialId()));
                        if (snEntity == null) throw new IllegalStateException("SN码不可出库：" + sn);
                        snEntity.setStatus("outbound");
                        snEntity.setOutboundCode(order.getCode());
                        snEntity.setOutboundTime(now);
                        snMapper.updateById(snEntity);
                    }
                }
            }
            if (itemExecutedQty.compareTo(BigDecimal.ZERO) > 0) {
                totalExecutedQty = totalExecutedQty.add(itemExecutedQty);
                item.setOutboundQty(outboundQty.add(itemExecutedQty));
                item.setUpdateTime(now);
                itemMapper.updateById(item);
            }
        }

        if (totalExecutedQty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("请至少填写一条出库数量");
        BigDecimal orderOutboundQty = nz(order.getOutboundQty()).add(totalExecutedQty);
        String status = orderOutboundQty.compareTo(nz(order.getTotalQty())) >= 0 ? "completed" : "partial";
        order.setOutboundQty(orderOutboundQty);
        order.setStatus(status);
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        return detail(orderId);
    }

    @Transactional
    public void delete(Long id) {
        recordMapper.delete(new LambdaQueryWrapper<OutboundRecord>().eq(OutboundRecord::getOrderId, id));
        itemMapper.delete(new LambdaQueryWrapper<OutboundItem>().eq(OutboundItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    private void decreaseInventory(OutboundItem item, OutboundOrder order, String locationCode, BigDecimal qty, LocalDateTime now) {
        Long materialId = item.getMaterialId();
        String warehouseCode = text(order.getWarehouseCode());
        String batchNo = text(item.getBatchNo());
        Inventory inventory = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getMaterialId, materialId)
                        .eq(Inventory::getWarehouseCode, warehouseCode)
                        .eq(Inventory::getLocationCode, locationCode)
                        .orderByAsc(Inventory::getId)).stream()
                .filter(inv -> batchNo.equals(text(inv.getBatchNo())))
                .findFirst().orElse(null);
        if (inventory == null && !StringUtils.hasText(batchNo)) {
            inventory = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getMaterialId, materialId)
                            .eq(Inventory::getWarehouseCode, warehouseCode)
                            .eq(Inventory::getLocationCode, locationCode)
                            .orderByAsc(Inventory::getId).last("LIMIT 1")).stream()
                    .findFirst().orElse(null);
        }
        if (inventory == null) throw new IllegalStateException("库存不存在，无法出库");
        if (nz(inventory.getAvailableQty()).compareTo(qty) < 0) throw new IllegalStateException("可用库存不足，无法出库");
        inventory.setQuantity(nz(inventory.getQuantity()).subtract(qty));
        inventory.setAvailableQty(nz(inventory.getAvailableQty()).subtract(qty));
        inventory.setUpdateTime(now);
        inventoryMapper.updateById(inventory);
    }

    private Long resolveMaterialId(Map<String, Object> item) {
        Object id = item.get("materialId");
        if (id instanceof Number number) return number.longValue();
        String sku = text(item.get("sku"));
        if (!StringUtils.hasText(sku)) throw new IllegalArgumentException("请填写SKU编码");
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>().eq(Material::getSku, sku).last("LIMIT 1"));
        if (material == null) throw new IllegalArgumentException("物料不存在：" + sku);
        return material.getId();
    }

    private Map<Long, Material> materialMap() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Warehouse> warehouseMap() {
        return warehouseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Warehouse::getCode, Function.identity(), (a, b) -> a));
    }

    private Map<String, Location> locationMap() {
        return locationMapper.selectList(null).stream()
                .collect(Collectors.toMap(Location::getCode, Function.identity(), (a, b) -> a));
    }

    private Map<Long, OutboundOrder> orderMap() {
        return orderMapper.selectList(null).stream()
                .collect(Collectors.toMap(OutboundOrder::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, InboundOrder> inboundOrderMap() {
        return inboundOrderMapper.selectList(null).stream()
                .collect(Collectors.toMap(InboundOrder::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Object> orderRow(OutboundOrder order, Map<String, Warehouse> warehouseMap) {
        Map<String, Object> result = new HashMap<>();
        BigDecimal totalQty = nz(order.getTotalQty());
        BigDecimal outboundQty = nz(order.getOutboundQty());
        Warehouse warehouse = warehouseMap.get(order.getWarehouseCode());
        result.put("id", order.getId());
        result.put("code", order.getCode());
        result.put("orderNo", order.getOrderNo());
        result.put("customer", order.getCustomer());
        result.put("warehouseCode", order.getWarehouseCode());
        result.put("warehouse", warehouse != null ? warehouse.getName() : order.getWarehouseCode());
        result.put("materialCount", order.getMaterialCount());
        result.put("totalQty", totalQty);
        result.put("outboundQty", outboundQty);
        result.put("outboundRate", totalQty.signum() == 0 ? 0 : outboundQty.multiply(BigDecimal.valueOf(100)).divide(totalQty, 0, java.math.RoundingMode.HALF_UP));
        result.put("totalAmount", order.getTotalAmount());
        result.put("status", order.getStatus());
        result.put("creator", order.getCreator());
        result.put("remark", order.getRemark());
        result.put("createTime", order.getCreateTime());
        return result;
    }

    private Map<String, Object> itemRow(OutboundItem item, Material material) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("materialId", item.getMaterialId());
        result.put("sku", material == null ? null : material.getSku());
        result.put("name", material == null ? null : material.getName());
        result.put("spec", material == null ? null : material.getSpec());
        result.put("locationCode", item.getLocationCode());
        result.put("batchNo", item.getBatchNo());
        result.put("method", item.getMethod());
        result.put("qty", item.getQuantity());
        result.put("outboundQty", item.getOutboundQty());
        result.put("unitPrice", item.getUnitPrice());
        return result;
    }

    private Map<String, Object> recordRow(OutboundRecord record, OutboundOrder order, Material material, Location location) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("materialId", record.getMaterialId());
        result.put("sku", material == null ? null : material.getSku());
        result.put("name", material == null ? null : material.getName());
        result.put("spec", material == null ? null : material.getSpec());
        result.put("locationCode", record.getLocationCode());
        result.put("batchNo", record.getBatchNo());
        result.put("method", record.getMethod());
        result.put("qty", record.getQuantity());
        result.put("outboundQty", null);
        result.put("unitPrice", record.getUnitPrice());
        result.put("outboundCode", order == null ? null : order.getCode());
        result.put("location", location != null ? location.getName() : record.getLocationCode());
        result.put("createTime", record.getCreateTime());
        result.put("operator", record.getOperator());
        return result;
    }

    private Map<String, Object> snRow(InboundSn sn, InboundOrder order, Material material, Location location) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", sn.getId());
        result.put("sn", sn.getSn());
        result.put("sku", material == null ? null : material.getSku());
        result.put("name", material == null ? null : material.getName());
        result.put("spec", material == null ? null : material.getSpec());
        result.put("location", location != null ? location.getName() : sn.getLocationCode());
        result.put("inboundCode", order == null ? null : order.getCode());
        result.put("inboundTime", sn.getInboundTime());
        result.put("outboundCode", sn.getOutboundCode());
        result.put("outboundTime", sn.getOutboundTime());
        result.put("status", sn.getStatus());
        return result;
    }

    private List<String> parseSnList(Object value) {
        String text = text(value);
        if (!StringUtils.hasText(text)) return List.of();
        return java.util.Arrays.stream(text.split("[,，\\n]")).map(String::trim).filter(StringUtils::hasText).distinct().toList();
    }

    private boolean contains(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword);
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

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
