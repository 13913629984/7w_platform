package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.InboundOrder;
import com.qws.common.entity.InboundRecord;
import com.qws.common.entity.Inventory;
import com.qws.common.entity.Material;
import com.qws.common.entity.MaterialType;
import com.qws.common.entity.OutboundOrder;
import com.qws.common.entity.OutboundRecord;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.InboundOrderMapper;
import com.qws.common.mapper.InboundRecordMapper;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.MaterialTypeMapper;
import com.qws.common.mapper.OutboundOrderMapper;
import com.qws.common.mapper.OutboundRecordMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存概览聚合服务：汇总物料、库存、出入库等关键指标，
 * 为 WMS 仪表盘提供统计卡片、库存分布与近 7 日出入库趋势数据。
 */
@Service
public class OverviewService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M/d");

    private final MaterialMapper materialMapper;
    private final MaterialTypeMapper materialTypeMapper;
    private final InventoryMapper inventoryMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final InboundRecordMapper inboundRecordMapper;
    private final OutboundRecordMapper outboundRecordMapper;
    private final WarehouseMapper warehouseMapper;

    public OverviewService(MaterialMapper materialMapper, MaterialTypeMapper materialTypeMapper,
                           InventoryMapper inventoryMapper, InboundOrderMapper inboundOrderMapper,
                           OutboundOrderMapper outboundOrderMapper, InboundRecordMapper inboundRecordMapper,
                           OutboundRecordMapper outboundRecordMapper, WarehouseMapper warehouseMapper) {
        this.materialMapper = materialMapper;
        this.materialTypeMapper = materialTypeMapper;
        this.inventoryMapper = inventoryMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.inboundRecordMapper = inboundRecordMapper;
        this.outboundRecordMapper = outboundRecordMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary());
        result.put("distribution", distribution());
        result.put("trend", trend(7));
        result.put("pendingInbound", pendingInbound());
        result.put("pendingOutbound", pendingOutbound());
        return result;
    }

    /** 顶部统计卡片：物料总数、当前库存总量、今日入库、今日出库（含较昨日变化）。 */
    private Map<String, Object> summary() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        long materialTotal = materialMapper.selectCount(null);
        long materialThisMonth = materialMapper.selectCount(new LambdaQueryWrapper<Material>()
                .ge(Material::getCreateTime, today.withDayOfMonth(1).atStartOfDay()));

        BigDecimal totalQty = inventoryMapper.selectList(null).stream()
                .map(inv -> inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long inboundToday = countOrdersByDay(inboundOrderMapper.selectList(null).stream()
                .map(InboundOrder::getCreateTime).toList(), today);
        long inboundYesterday = countOrdersByDay(inboundOrderMapper.selectList(null).stream()
                .map(InboundOrder::getCreateTime).toList(), yesterday);
        long outboundToday = countOrdersByDay(outboundOrderMapper.selectList(null).stream()
                .map(OutboundOrder::getCreateTime).toList(), today);
        long outboundYesterday = countOrdersByDay(outboundOrderMapper.selectList(null).stream()
                .map(OutboundOrder::getCreateTime).toList(), yesterday);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("materialTotal", materialTotal);
        summary.put("materialThisMonth", materialThisMonth);
        summary.put("totalQty", totalQty);
        summary.put("inboundToday", inboundToday);
        summary.put("inboundDelta", inboundToday - inboundYesterday);
        summary.put("outboundToday", outboundToday);
        summary.put("outboundDelta", outboundToday - outboundYesterday);
        return summary;
    }

    /** 库存分布：按物料类别汇总当前库存数量。 */
    private List<Map<String, Object>> distribution() {
        Map<Long, String> materialCategory = materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getId, m -> m.getCategory() == null ? "" : m.getCategory(), (a, b) -> a));
        Map<String, String> categoryNames = materialTypeMapper.selectList(
                        new LambdaQueryWrapper<MaterialType>().orderByAsc(MaterialType::getSort)).stream()
                .collect(Collectors.toMap(MaterialType::getCode, MaterialType::getName, (a, b) -> a, LinkedHashMap::new));

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Inventory inv : inventoryMapper.selectList(null)) {
            String category = materialCategory.getOrDefault(inv.getMaterialId(), "");
            BigDecimal qty = inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity();
            byCategory.merge(category, qty, BigDecimal::add);
        }

        List<Map<String, Object>> distribution = new ArrayList<>();
        byCategory.forEach((code, qty) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", code);
            item.put("name", categoryNames.getOrDefault(code, code == null || code.isEmpty() ? "其他" : code));
            item.put("quantity", qty);
            distribution.add(item);
        });
        distribution.sort((a, b) -> ((BigDecimal) b.get("quantity")).compareTo((BigDecimal) a.get("quantity")));
        return distribution;
    }

    /** 近 N 日出入库趋势：按操作记录的日期统计入库 / 出库笔数。 */
    private List<Map<String, Object>> trend(int days) {
        Map<LocalDate, Long> inboundByDay = inboundRecordMapper.selectList(null).stream()
                .map(InboundRecord::getCreateTime).filter(t -> t != null)
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));
        Map<LocalDate, Long> outboundByDay = outboundRecordMapper.selectList(null).stream()
                .map(OutboundRecord::getCreateTime).filter(t -> t != null)
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.format(DAY_LABEL));
            point.put("inbound", inboundByDay.getOrDefault(day, 0L));
            point.put("outbound", outboundByDay.getOrDefault(day, 0L));
            trend.add(point);
        }
        return trend;
    }

    private long countOrdersByDay(List<LocalDateTime> times, LocalDate day) {
        return times.stream().filter(t -> t != null && t.toLocalDate().equals(day)).count();
    }

    /** 待入库列表：未完成（待入库 / 部分入库）的入库单。 */
    private List<Map<String, Object>> pendingInbound() {
        Map<String, String> warehouseNames = warehouseNames();
        return inboundOrderMapper.selectList(new LambdaQueryWrapper<InboundOrder>()
                        .ne(InboundOrder::getStatus, "completed")
                        .orderByDesc(InboundOrder::getCreateTime).orderByDesc(InboundOrder::getId)
                        .last("LIMIT 8")).stream()
                .map(order -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", order.getId());
                    row.put("code", order.getCode());
                    row.put("supplier", order.getSupplier());
                    row.put("warehouse", warehouseNames.getOrDefault(order.getWarehouseCode(), order.getWarehouseCode()));
                    row.put("totalQty", order.getTotalQty() == null ? BigDecimal.ZERO : order.getTotalQty());
                    row.put("status", order.getStatus());
                    row.put("createTime", order.getCreateTime());
                    return row;
                }).toList();
    }

    /** 待出库列表：未完成（待出库 / 部分出库）的出库单。 */
    private List<Map<String, Object>> pendingOutbound() {
        Map<String, String> warehouseNames = warehouseNames();
        return outboundOrderMapper.selectList(new LambdaQueryWrapper<OutboundOrder>()
                        .ne(OutboundOrder::getStatus, "completed")
                        .orderByDesc(OutboundOrder::getCreateTime).orderByDesc(OutboundOrder::getId)
                        .last("LIMIT 8")).stream()
                .map(order -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", order.getId());
                    row.put("code", order.getCode());
                    row.put("customer", order.getCustomer());
                    row.put("warehouse", warehouseNames.getOrDefault(order.getWarehouseCode(), order.getWarehouseCode()));
                    row.put("totalQty", order.getTotalQty() == null ? BigDecimal.ZERO : order.getTotalQty());
                    row.put("status", order.getStatus());
                    row.put("createTime", order.getCreateTime());
                    return row;
                }).toList();
    }

    private Map<String, String> warehouseNames() {
        return warehouseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Warehouse::getCode, Warehouse::getName, (a, b) -> a));
    }
}
