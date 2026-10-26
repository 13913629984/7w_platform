package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.order.OrderBatchDeleteRequest;
import com.qws.common.dto.order.OrderDeleteRequest;
import com.qws.common.dto.order.OrderItemDTO;
import com.qws.common.dto.order.OrderListResult;
import com.qws.common.dto.order.OrderRequest;
import com.qws.common.dto.order.OrderVO;
import com.qws.common.entity.CrmCustomer;
import com.qws.common.entity.CrmDeal;
import com.qws.common.entity.CrmOrder;
import com.qws.common.entity.CrmOrderItem;
import com.qws.common.entity.Inventory;
import com.qws.common.entity.Location;
import com.qws.common.entity.Material;
import com.qws.common.entity.OutboundItem;
import com.qws.common.entity.OutboundOrder;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.CrmCustomerMapper;
import com.qws.common.mapper.CrmDealMapper;
import com.qws.common.mapper.CrmOrderItemMapper;
import com.qws.common.mapper.CrmOrderMapper;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.LocationMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.OutboundItemMapper;
import com.qws.common.mapper.OutboundOrderMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CrmOrderService {

    private static final List<String> STATUSES = List.of("待确认", "已确认", "已发货", "已完成", "已取消");
    // 待发货：已确认但尚未发货的订单
    private static final List<String> PENDING_DELIVER = List.of("已确认");

    private final CrmOrderMapper orderMapper;
    private final CrmOrderItemMapper itemMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmDealMapper dealMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundItemMapper outboundItemMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;
    private final LocationMapper locationMapper;

    public CrmOrderService(CrmOrderMapper orderMapper,
                           CrmOrderItemMapper itemMapper,
                           CrmCustomerMapper customerMapper,
                           CrmDealMapper dealMapper,
                           OutboundOrderMapper outboundOrderMapper,
                           OutboundItemMapper outboundItemMapper,
                           MaterialMapper materialMapper,
                           WarehouseMapper warehouseMapper,
                           InventoryMapper inventoryMapper,
                           LocationMapper locationMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.customerMapper = customerMapper;
        this.dealMapper = dealMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundItemMapper = outboundItemMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryMapper = inventoryMapper;
        this.locationMapper = locationMapper;
    }

    public OrderListResult list(String keyword, String status, Long customerId, int page, int pageSize) {
        LambdaQueryWrapper<CrmOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmOrder::getOrderNo, keyword)
                    .or().like(CrmOrder::getCustomerName, keyword)
                    .or().like(CrmOrder::getDealName, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CrmOrder::getStatus, status);
        }
        if (customerId != null) {
            wrapper.eq(CrmOrder::getCustomerId, customerId);
        }
        wrapper.orderByDesc(CrmOrder::getSignDate).orderByDesc(CrmOrder::getId);
        Page<CrmOrder> result = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<OrderVO> vos = result.getRecords().stream().map(this::toVO).toList();
        OrderListResult payload = new OrderListResult();
        payload.setRows(vos);
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        payload.setStatuses(STATUSES);
        return payload;
    }

    public OrderVO detail(Long id) {
        CrmOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return toVO(order);
    }

    /** 生成下一个订单编号：SO + yyyyMMdd + 3位流水 */
    public String nextOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "SO" + dateStr;
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<CrmOrder>()
                .likeRight(CrmOrder::getOrderNo, prefix));
        long seq = (count == null ? 0 : count) + 1;
        return prefix + String.format("%03d", seq);
    }

    @Transactional
    public OrderVO create(OrderRequest request) {
        validate(request);
        CrmOrder order = new CrmOrder();
        applyRequest(order, request);
        if (!StringUtils.hasText(order.getOrderNo())) {
            order.setOrderNo(nextOrderNo());
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus("待确认");
        }
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        saveItems(order.getId(), request.getItems());
        return toVO(order);
    }

    @Transactional
    public OrderVO update(OrderRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的订单");
        }
        validate(request);
        CrmOrder order = orderMapper.selectById(request.getId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        applyRequest(order, request);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        deleteItems(order.getId());
        saveItems(order.getId(), request.getItems());
        return toVO(order);
    }

    @Transactional
    public void delete(OrderDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的订单");
        }
        CrmOrder order = orderMapper.selectById(request.getId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        orderMapper.deleteById(request.getId());
        deleteItems(request.getId());
    }

    @Transactional
    public int batchDelete(OrderBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }
        for (Long id : request.getIds()) {
            deleteItems(id);
        }
        return orderMapper.deleteByIds(request.getIds());
    }

    /** 确认订单：待确认 -> 已确认 */
    public OrderVO confirm(Long id) {
        CrmOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!"待确认".equals(order.getStatus())) {
            throw new IllegalStateException("仅待确认订单可执行确认操作");
        }
        order.setStatus("已确认");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(order);
    }

    /** 生成出库单：已确认 -> 已发货，并在 WMS 中创建真实出库单 */
    @Transactional
    public OrderVO deliver(Long id) {
        CrmOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (StringUtils.hasText(order.getOutboundCode())) {
            throw new IllegalStateException("该订单已生成出库单：" + order.getOutboundCode());
        }
        if (!"已确认".equals(order.getStatus())) {
            throw new IllegalStateException("仅已确认订单可生成出库单");
        }
        // 在 WMS 中创建真实出库单（CRM 与 WMS 共用同一数据库与 common mapper）
        String outboundCode = createWmsOutbound(order);
        order.setStatus("已发货");
        order.setOutboundCode(outboundCode);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(order);
    }

    /** 依据 CRM 订单及其产品明细在 WMS 生成出库单，返回出库单号 */
    private String createWmsOutbound(CrmOrder order) {
        LocalDateTime now = LocalDateTime.now();
        List<CrmOrderItem> orderItems = itemMapper.selectList(new LambdaQueryWrapper<CrmOrderItem>()
                .eq(CrmOrderItem::getOrderId, order.getId())
                .orderByAsc(CrmOrderItem::getId));
        if (orderItems.isEmpty()) {
            throw new IllegalStateException("订单没有产品明细，无法生成出库单");
        }

        // 1. 订单产品 -> WMS 物料（匹配不到直接报错，不再拿首个物料兜底）
        List<ResolvedOrderLine> lines = new ArrayList<>();
        for (CrmOrderItem item : orderItems) {
            Material material = resolveMaterial(item);
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getQuantity());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("订单产品「" + item.getProductName() + "」数量必须大于 0");
            }
            lines.add(new ResolvedOrderLine(item, material, qty,
                    item.getPrice() == null ? BigDecimal.ZERO : item.getPrice()));
        }

        // 2. 选一个能整体满足订单的仓库（出库单头只有一个仓库字段，不能跨仓拆单）
        AllocationPlan plan = planAllocation(lines);

        // 3. 落库：单头 + 按「物料+库位+批次」拆分的明细（明细自带库位，执行出库才能直接扣库存）
        String outboundCode = nextOutboundCode();
        OutboundOrder outbound = new OutboundOrder();
        outbound.setCode(outboundCode);
        outbound.setOrderNo(order.getOrderNo());
        outbound.setCustomer(order.getCustomerName());
        outbound.setWarehouseCode(plan.warehouseCode());
        outbound.setMaterialCount(plan.lines().size());

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (AllocationLine line : plan.lines()) {
            totalQty = totalQty.add(line.qty());
            totalAmount = totalAmount.add(line.qty().multiply(line.unitPrice()));
        }
        outbound.setTotalQty(totalQty);
        outbound.setOutboundQty(BigDecimal.ZERO);
        outbound.setTotalAmount(totalAmount);
        outbound.setStatus("pending");
        outbound.setCreator(StringUtils.hasText(order.getOwner()) ? order.getOwner() : "CRM系统");
        outbound.setRemark("由销售订单 " + order.getOrderNo() + " 自动生成");
        outbound.setCreateTime(now);
        outbound.setUpdateTime(now);
        outboundOrderMapper.insert(outbound);

        for (AllocationLine line : plan.lines()) {
            OutboundItem outItem = new OutboundItem();
            outItem.setOrderId(outbound.getId());
            outItem.setMaterialId(line.materialId());
            outItem.setLocationCode(line.locationCode());
            outItem.setBatchNo(line.batchNo());
            outItem.setMethod("qty");
            outItem.setQuantity(line.qty());
            outItem.setOutboundQty(BigDecimal.ZERO);
            outItem.setUnitPrice(line.unitPrice());
            outItem.setRemark(line.productName());
            outItem.setCreateTime(now);
            outItem.setUpdateTime(now);
            outboundItemMapper.insert(outItem);
        }
        return outboundCode;
    }

    /**
     * 为订单计算可执行的出库分配方案。
     *
     * <p>出库单头只有一个 warehouseCode，因此必须挑一个「所有明细都能被满足」的仓库；
     * 找不到就抛出带数量明细的业务异常，避免生成一张注定执行失败的出库单。
     */
    private AllocationPlan planAllocation(List<ResolvedOrderLine> lines) {
        Map<Long, BigDecimal> need = new HashMap<>();
        for (ResolvedOrderLine line : lines) {
            need.merge(line.material().getId(), line.qty(), BigDecimal::add);
        }
        List<Inventory> rawStock = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .in(Inventory::getMaterialId, new ArrayList<>(need.keySet()))
                .eq(Inventory::getStatus, 1)
                .gt(Inventory::getAvailableQty, BigDecimal.ZERO)
                .orderByAsc(Inventory::getId));
        // 过滤掉库位不存在的脏库存行（历史数据里落过 location_code='DEFAULT' 这种幽灵行）
        Map<String, Location> locationMap = locationMapper.selectList(null).stream()
                .collect(Collectors.toMap(Location::getCode, Function.identity(), (a, b) -> a));
        List<Inventory> stock = rawStock.stream()
                .filter(inv -> {
                    Location location = locationMap.get(inv.getLocationCode());
                    return location != null && inv.getWarehouseCode().equals(location.getWarehouseCode());
                })
                .collect(Collectors.toList());
        if (stock.isEmpty()) {
            throw new IllegalStateException("订单产品在 WMS 没有可用库存，请先采购入库或改为手工创建出库单");
        }

        // 候选仓库：优先成品仓
        Map<String, Warehouse> warehouseMap = warehouseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Warehouse::getCode, Function.identity(), (a, b) -> a));
        List<String> candidates = stock.stream().map(Inventory::getWarehouseCode).distinct().collect(Collectors.toList());
        candidates.sort((a, b) -> {
            int rankA = isFinished(warehouseMap.get(a)) ? 0 : 1;
            int rankB = isFinished(warehouseMap.get(b)) ? 0 : 1;
            return rankA != rankB ? Integer.compare(rankA, rankB) : a.compareTo(b);
        });

        String best = null;
        String shortageDetail = null;
        for (String warehouseCode : candidates) {
            Map<Long, BigDecimal> free = new HashMap<>();
            for (Inventory inv : stock) {
                if (!warehouseCode.equals(inv.getWarehouseCode())) continue;
                free.merge(inv.getMaterialId(), nz(inv.getAvailableQty()), BigDecimal::add);
            }
            List<String> shortages = new ArrayList<>();
            for (ResolvedOrderLine line : lines) {
                BigDecimal available = free.getOrDefault(line.material().getId(), BigDecimal.ZERO);
                if (available.compareTo(line.qty()) < 0) {
                    shortages.add(line.material().getName() + " 需要 " + plain(line.qty()) + "，可用 " + plain(available));
                }
            }
            if (shortages.isEmpty()) {
                best = warehouseCode;
                break;
            }
            if (shortageDetail == null) {
                shortageDetail = "仓库 " + warehouseCode + "：" + String.join("；", shortages);
            }
        }
        if (best == null) {
            throw new IllegalStateException("库存不足，无法生成出库单（" + shortageDetail + "）");
        }

        // 按先进先出把每个物料的需求量摊到该仓库的库存行上
        Map<Long, BigDecimal> remaining = new HashMap<>(need);
        List<AllocationLine> result = new ArrayList<>();
        for (Inventory inv : stock) {
            if (!best.equals(inv.getWarehouseCode())) continue;
            BigDecimal left = remaining.getOrDefault(inv.getMaterialId(), BigDecimal.ZERO);
            if (left.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal take = nz(inv.getAvailableQty()).min(left);
            if (take.compareTo(BigDecimal.ZERO) <= 0) continue;
            ResolvedOrderLine line = lines.stream()
                    .filter(l -> l.material().getId().equals(inv.getMaterialId()))
                    .findFirst().orElseThrow();
            result.add(new AllocationLine(inv.getMaterialId(), inv.getLocationCode(),
                    inv.getBatchNo() == null ? "" : inv.getBatchNo(), take, line.unitPrice(), line.item().getProductName()));
            remaining.put(inv.getMaterialId(), left.subtract(take));
        }
        return new AllocationPlan(best, result);
    }

    private static boolean isFinished(Warehouse warehouse) {
        return warehouse != null && "finished".equals(warehouse.getType());
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return nz(value).stripTrailingZeros().toPlainString();
    }

    /** 订单行 + 解析后的物料 */
    private record ResolvedOrderLine(CrmOrderItem item, Material material, BigDecimal qty, BigDecimal unitPrice) {
    }

    /** 单条出库明细（物料 + 库位 + 批次 + 数量） */
    private record AllocationLine(Long materialId, String locationCode, String batchNo,
                                  BigDecimal qty, BigDecimal unitPrice, String productName) {
    }

    /** 出库分配方案：单头仓库 + 明细列表 */
    private record AllocationPlan(String warehouseCode, List<AllocationLine> lines) {
    }

    /** 生成下一个出库单号：OUT + yyyyMM + 3位流水（与 WMS 既有格式一致） */
    private String nextOutboundCode() {
        String prefix = "OUT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        Long count = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>()
                .likeRight(OutboundOrder::getCode, prefix));
        long seq = (count == null ? 0 : count) + 1;
        return prefix + String.format("%03d", seq);
    }

    /** 按产品编码/名称匹配 WMS 物料，匹配不到直接报错（不再拿首个物料兜底） */
    private Material resolveMaterial(CrmOrderItem item) {
        if (StringUtils.hasText(item.getProductCode())) {
            Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                    .eq(Material::getSku, item.getProductCode().trim()).last("LIMIT 1"));
            if (material != null) {
                return material;
            }
        }
        if (StringUtils.hasText(item.getProductName())) {
            Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                    .eq(Material::getName, item.getProductName().trim()).last("LIMIT 1"));
            if (material != null) {
                return material;
            }
        }
        throw new IllegalStateException("订单产品「" + item.getProductName() + "（" + item.getProductCode()
                + "）」未匹配到 WMS 物料，请先在物料管理中维护该产品编码");
    }

    private void validate(OrderRequest request) {
        if (StringUtils.hasText(request.getOrderNo()) && request.getOrderNo().length() > 40) {
            throw new IllegalArgumentException("订单编号长度不能超过40个字符");
        }
        if (!StringUtils.hasText(request.getCustomerName()) && request.getCustomerId() == null) {
            throw new IllegalArgumentException("请选择客户");
        }
        if (!StringUtils.hasText(request.getContractNo())) {
            throw new IllegalArgumentException("请输入合同编号");
        }
        if (!StringUtils.hasText(request.getSignDate())) {
            throw new IllegalArgumentException("请选择签订日期");
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("订单金额不能为负数");
        }
        // 订单编号唯一性校验
        String orderNo = request.getOrderNo();
        if (StringUtils.hasText(orderNo)) {
            CrmOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<CrmOrder>()
                    .eq(CrmOrder::getOrderNo, orderNo.trim()).last("LIMIT 1"));
            if (existing != null && !existing.getId().equals(request.getId())) {
                throw new IllegalArgumentException("订单编号已存在");
            }
        }
    }

    private void applyRequest(CrmOrder order, OrderRequest request) {
        order.setOrderNo(StringUtils.hasText(request.getOrderNo()) ? request.getOrderNo().trim() : order.getOrderNo());
        order.setContractNo(request.getContractNo());
        order.setOwner(request.getOwner());
        order.setSignDate(parseDate(request.getSignDate()));
        order.setExpectDeliverAt(parseDate(request.getExpectDeliverAt()));
        order.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getStatus())) {
            order.setStatus(request.getStatus());
        }
        // 解析关联客户
        Long customerId = request.getCustomerId();
        String customerName = request.getCustomerName();
        if (customerId != null) {
            CrmCustomer customer = customerMapper.selectById(customerId);
            if (customer != null) {
                customerName = customer.getName();
            }
        } else if (StringUtils.hasText(customerName)) {
            CrmCustomer customer = customerMapper.selectOne(new LambdaQueryWrapper<CrmCustomer>()
                    .eq(CrmCustomer::getName, customerName.trim()).last("LIMIT 1"));
            if (customer != null) {
                customerId = customer.getId();
            }
        }
        order.setCustomerId(customerId);
        order.setCustomerName(StringUtils.hasText(customerName) ? customerName.trim() : null);
        // 解析关联商机
        Long dealId = request.getDealId();
        String dealName = request.getDealName();
        if (dealId != null) {
            CrmDeal deal = dealMapper.selectById(dealId);
            if (deal != null) {
                dealName = deal.getName();
            }
        }
        order.setDealId(dealId);
        order.setDealName(StringUtils.hasText(dealName) ? dealName.trim() : null);
        // 金额：优先取明细合计，否则取传入金额
        BigDecimal itemsTotal = computeItemsTotal(request.getItems());
        if (itemsTotal.compareTo(BigDecimal.ZERO) > 0) {
            order.setAmount(itemsTotal);
        } else {
            order.setAmount(request.getAmount() == null ? BigDecimal.ZERO : request.getAmount());
        }
    }

    private BigDecimal computeItemsTotal(List<OrderItemDTO> items) {
        if (items == null) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(i -> StringUtils.hasText(i.getProductName()) || StringUtils.hasText(i.getProductCode()))
                .map(this::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal lineSubtotal(OrderItemDTO item) {
        BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        int qty = item.getQuantity() == null ? 0 : item.getQuantity();
        return price.multiply(BigDecimal.valueOf(qty));
    }

    private void saveItems(Long orderId, List<OrderItemDTO> items) {
        if (items == null) {
            return;
        }
        for (OrderItemDTO dto : items) {
            if (!StringUtils.hasText(dto.getProductName()) && !StringUtils.hasText(dto.getProductCode())) {
                continue;
            }
            CrmOrderItem item = new CrmOrderItem();
            item.setOrderId(orderId);
            item.setProductCode(dto.getProductCode());
            item.setProductName(dto.getProductName());
            item.setSpec(dto.getSpec());
            item.setQuantity(dto.getQuantity() == null ? 0 : dto.getQuantity());
            item.setPrice(dto.getPrice() == null ? BigDecimal.ZERO : dto.getPrice());
            item.setSubtotal(lineSubtotal(dto));
            itemMapper.insert(item);
        }
    }

    private void deleteItems(Long orderId) {
        itemMapper.delete(new LambdaQueryWrapper<CrmOrderItem>().eq(CrmOrderItem::getOrderId, orderId));
    }

    private OrderVO toVO(CrmOrder order) {
        List<CrmOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<CrmOrderItem>()
                .eq(CrmOrderItem::getOrderId, order.getId())
                .orderByAsc(CrmOrderItem::getId));
        return OrderVO.from(order, items);
    }

    private Map<String, Object> computeStats() {
        List<CrmOrder> all = orderMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        int total = all.size();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        long monthAdded = all.stream()
                .filter(o -> o.getCreateTime() != null && !o.getCreateTime().toLocalDate().isBefore(firstDayOfMonth))
                .count();
        BigDecimal totalAmount = all.stream()
                .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingDeliver = all.stream()
                .filter(o -> PENDING_DELIVER.contains(o.getStatus()))
                .count();
        stats.put("total", total);
        stats.put("monthAdded", monthAdded);
        stats.put("totalAmount", totalAmount.stripTrailingZeros().toPlainString());
        stats.put("pendingDeliver", pendingDeliver);
        return stats;
    }

    public List<Map<String, Object>> customerOptions() {
        return customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
                        .orderByDesc(CrmCustomer::getUpdateTime)).stream()
                .map(c -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", c.getId());
                    option.put("name", c.getName());
                    return option;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> dealOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (CrmDeal deal : dealMapper.selectList(new LambdaQueryWrapper<CrmDeal>().orderByDesc(CrmDeal::getId))) {
            Map<String, Object> option = new HashMap<>();
            option.put("id", deal.getId());
            option.put("name", deal.getName());
            option.put("customerId", deal.getCustomerId());
            option.put("customerName", deal.getCustomerName());
            option.put("amount", deal.getAmount());
            options.add(option);
        }
        return options;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            String trimmed = value.trim();
            if (trimmed.length() > 10) {
                trimmed = trimmed.substring(0, 10);
            }
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ex) {
            return null;
        }
    }
}
