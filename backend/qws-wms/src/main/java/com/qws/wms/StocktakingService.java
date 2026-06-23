package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.Inventory;
import com.qws.common.entity.Location;
import com.qws.common.entity.Material;
import com.qws.common.entity.StockTakingOrder;
import com.qws.common.entity.StockTakingTask;
import com.qws.common.entity.Warehouse;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.LocationMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.StockTakingOrderMapper;
import com.qws.common.mapper.StockTakingTaskMapper;
import com.qws.common.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StocktakingService {

    private final StockTakingOrderMapper orderMapper;
    private final StockTakingTaskMapper taskMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;

    public StocktakingService(StockTakingOrderMapper orderMapper, StockTakingTaskMapper taskMapper,
                              InventoryMapper inventoryMapper, MaterialMapper materialMapper,
                              WarehouseMapper warehouseMapper, LocationMapper locationMapper) {
        this.orderMapper = orderMapper;
        this.taskMapper = taskMapper;
        this.inventoryMapper = inventoryMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.locationMapper = locationMapper;
    }

    public List<Map<String, Object>> list(String keyword, String status) {
        LambdaQueryWrapper<StockTakingOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) wrapper.eq(StockTakingOrder::getStatus, status);
        wrapper.orderByDesc(StockTakingOrder::getCreateTime).orderByDesc(StockTakingOrder::getId);
        Map<String, Warehouse> warehouseMap = warehouseMap();
        List<Map<String, Object>> rows = orderMapper.selectList(wrapper).stream()
                .map(o -> orderRow(o, warehouseMap)).collect(Collectors.toList());
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            rows = rows.stream().filter(r -> contains(r.get("code"), kw)
                    || contains(r.get("warehouse"), kw)
                    || contains(r.get("scopeDesc"), kw)).collect(Collectors.toList());
        }
        return rows;
    }

    public Map<String, Object> stats() {
        List<StockTakingOrder> orders = orderMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", orders.size());
        stats.put("completed", orders.stream().filter(o -> "completed".equals(text(o.getStatus()))).count());
        stats.put("processing", orders.stream().filter(o -> "processing".equals(text(o.getStatus()))).count());
        stats.put("pendingReview", orders.stream().filter(o -> "pending_review".equals(text(o.getStatus()))).count());
        return stats;
    }

    public Map<String, Object> detail(Long id) {
        StockTakingOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("盘点单不存在");
        Map<String, Object> result = orderRow(order, warehouseMap());
        result.put("tasks", taskRows(id));
        return result;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        String code = defaultText(request.get("code"), generateCode());
        String warehouseCode = text(request.get("warehouseCode"));
        String type = defaultText(request.get("type"), "full");
        String creator = text(request.get("creator"));
        if (!StringUtils.hasText(warehouseCode)) throw new IllegalArgumentException("请选择盘点仓库");
        if (!StringUtils.hasText(creator)) throw new IllegalArgumentException("请填写盘点人");
        if (orderMapper.selectCount(new LambdaQueryWrapper<StockTakingOrder>().eq(StockTakingOrder::getCode, code)) > 0) {
            throw new IllegalStateException("盘点单号已存在");
        }
        List<String> locationCodes = parseLocationCodes(request.get("locationCodes"));
        String scopeDesc = defaultText(request.get("scopeDesc"), scopeDescOf(warehouseCode, locationCodes));

        LocalDateTime now = LocalDateTime.now();
        StockTakingOrder order = new StockTakingOrder();
        order.setCode(code);
        order.setWarehouseCode(warehouseCode);
        order.setScopeDesc(scopeDesc);
        order.setType(type);
        order.setStatus("pending");
        order.setCreator(creator);
        order.setRemark(text(request.get("remark")));
        order.setTaskCount(0);
        order.setCompletedCount(0);
        order.setDiffCount(0);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);

        generateTasks(order, locationCodes, now);
        refreshOrderCounts(order, now);
        return detail(order.getId());
    }

    @Transactional
    public Map<String, Object> start(Long id) {
        StockTakingOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("盘点单不存在");
        if (!"pending".equals(text(order.getStatus()))) throw new IllegalStateException("仅待开始的盘点单可以启动");
        LocalDateTime now = LocalDateTime.now();
        if (taskMapper.selectCount(new LambdaQueryWrapper<StockTakingTask>().eq(StockTakingTask::getOrderId, id)) == 0) {
            generateTasks(order, List.of(), now);
        }
        order.setStatus("processing");
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        refreshOrderCounts(order, now);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> process(Map<String, Object> request) {
        Long orderId = longValue(request.get("id"), "缺少盘点单ID");
        Long taskId = longValue(request.get("taskId"), "缺少盘点任务ID");
        StockTakingOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("盘点单不存在");
        if ("completed".equals(text(order.getStatus()))) throw new IllegalStateException("盘点单已结束");
        StockTakingTask task = taskMapper.selectOne(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getId, taskId).eq(StockTakingTask::getOrderId, orderId));
        if (task == null) throw new IllegalArgumentException("盘点任务不存在");
        if (request.get("actualQty") == null) throw new IllegalArgumentException("请填写实盘数量");
        BigDecimal actualQty = decimal(request.get("actualQty"));
        if (actualQty.signum() < 0) throw new IllegalArgumentException("实盘数量不能小于0");

        LocalDateTime now = LocalDateTime.now();
        BigDecimal bookQty = nz(task.getBookQty());
        BigDecimal diffQty = actualQty.subtract(bookQty);
        boolean hasDiff = diffQty.signum() != 0;
        task.setActualQty(actualQty);
        task.setDiffQty(diffQty);
        task.setDiffAmount(diffQty.multiply(nz(task.getUnitPrice())).setScale(2, java.math.RoundingMode.HALF_UP));
        task.setHasDiff(hasDiff ? 1 : 0);
        task.setStatus("completed");
        task.setReviewStatus(hasDiff ? "pending" : "");
        task.setRemark(text(request.get("remark")));
        task.setUpdateTime(now);
        taskMapper.updateById(task);

        if (allTasksCompleted(orderId)) {
            order.setStatus(hasDiffTasks(orderId) ? "pending_review" : "completed");
        } else {
            order.setStatus("processing");
        }
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        refreshOrderCounts(order, now);
        return detail(orderId);
    }

    @Transactional
    public Map<String, Object> review(Map<String, Object> request) {
        Long orderId = longValue(request.get("id"), "缺少盘点单ID");
        Long taskId = longValue(request.get("taskId"), "缺少盘点任务ID");
        String result = text(request.get("result"));
        if (!"approved".equals(result) && !"rejected".equals(result)) throw new IllegalArgumentException("请选择审核结果");
        StockTakingOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("盘点单不存在");
        StockTakingTask task = taskMapper.selectOne(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getId, taskId).eq(StockTakingTask::getOrderId, orderId));
        if (task == null) throw new IllegalArgumentException("盘点任务不存在");
        if (task.getHasDiff() == null || task.getHasDiff() != 1) throw new IllegalStateException("无差异任务无需审核");

        LocalDateTime now = LocalDateTime.now();
        task.setReviewStatus(result);
        if (StringUtils.hasText(text(request.get("remark")))) task.setRemark(text(request.get("remark")));
        task.setUpdateTime(now);
        taskMapper.updateById(task);
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        return detail(orderId);
    }

    @Transactional
    public Map<String, Object> complete(Long id) {
        StockTakingOrder order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("盘点单不存在");
        if ("completed".equals(text(order.getStatus()))) throw new IllegalStateException("盘点单已结束");
        if (!allTasksCompleted(id)) throw new IllegalStateException("存在未盘点的任务，无法结束盘点");
        List<StockTakingTask> diffTasks = taskMapper.selectList(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getOrderId, id).eq(StockTakingTask::getHasDiff, 1));
        boolean unreviewed = diffTasks.stream().anyMatch(t -> "pending".equals(text(t.getReviewStatus())));
        if (unreviewed) throw new IllegalStateException("存在未审核的差异项，请先完成审核");

        LocalDateTime now = LocalDateTime.now();
        for (StockTakingTask task : diffTasks) {
            if ("approved".equals(text(task.getReviewStatus()))) {
                adjustInventory(task, now);
            }
        }
        order.setStatus("completed");
        order.setUpdateTime(now);
        orderMapper.updateById(order);
        refreshOrderCounts(order, now);
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        taskMapper.delete(new LambdaQueryWrapper<StockTakingTask>().eq(StockTakingTask::getOrderId, id));
        orderMapper.deleteById(id);
    }

    private void generateTasks(StockTakingOrder order, List<String> locationCodes, LocalDateTime now) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, order.getWarehouseCode());
        if (locationCodes != null && !locationCodes.isEmpty()) wrapper.in(Inventory::getLocationCode, locationCodes);
        wrapper.orderByAsc(Inventory::getLocationCode).orderByAsc(Inventory::getId);
        List<Inventory> inventories = inventoryMapper.selectList(wrapper);
        Map<Long, Material> materialMap = materialMap();
        for (Inventory inventory : inventories) {
            Material material = materialMap.get(inventory.getMaterialId());
            StockTakingTask task = new StockTakingTask();
            task.setOrderId(order.getId());
            task.setInventoryId(inventory.getId());
            task.setMaterialId(inventory.getMaterialId());
            task.setLocationCode(inventory.getLocationCode());
            task.setBatchNo(inventory.getBatchNo());
            task.setBookQty(nz(inventory.getQuantity()));
            task.setActualQty(BigDecimal.ZERO);
            task.setDiffQty(BigDecimal.ZERO);
            task.setDiffAmount(BigDecimal.ZERO);
            task.setHasDiff(0);
            task.setUnitPrice(material != null && material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO);
            task.setStatus("pending");
            task.setReviewStatus("");
            task.setCreateTime(now);
            task.setUpdateTime(now);
            taskMapper.insert(task);
        }
    }

    private void adjustInventory(StockTakingTask task, LocalDateTime now) {
        if (task.getInventoryId() == null) return;
        Inventory inventory = inventoryMapper.selectById(task.getInventoryId());
        if (inventory == null) return;
        BigDecimal actualQty = nz(task.getActualQty());
        BigDecimal diffQty = nz(task.getDiffQty());
        BigDecimal available = nz(inventory.getAvailableQty()).add(diffQty);
        if (available.signum() < 0) available = BigDecimal.ZERO;
        inventory.setQuantity(actualQty);
        inventory.setAvailableQty(available);
        inventory.setRemark("盘点调整：差异 " + diffQty.toPlainString());
        inventory.setUpdateTime(now);
        inventoryMapper.updateById(inventory);
    }

    private void refreshOrderCounts(StockTakingOrder order, LocalDateTime now) {
        List<StockTakingTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getOrderId, order.getId()));
        int taskCount = tasks.size();
        int completed = (int) tasks.stream().filter(t -> "completed".equals(text(t.getStatus()))).count();
        int diff = (int) tasks.stream().filter(t -> t.getHasDiff() != null && t.getHasDiff() == 1).count();
        order.setTaskCount(taskCount);
        order.setCompletedCount(completed);
        order.setDiffCount(diff);
        order.setUpdateTime(now);
        orderMapper.updateById(order);
    }

    private boolean allTasksCompleted(Long orderId) {
        long total = taskMapper.selectCount(new LambdaQueryWrapper<StockTakingTask>().eq(StockTakingTask::getOrderId, orderId));
        if (total == 0) return false;
        long pending = taskMapper.selectCount(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getOrderId, orderId).ne(StockTakingTask::getStatus, "completed"));
        return pending == 0;
    }

    private boolean hasDiffTasks(Long orderId) {
        return taskMapper.selectCount(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getOrderId, orderId).eq(StockTakingTask::getHasDiff, 1)) > 0;
    }

    private List<Map<String, Object>> taskRows(Long orderId) {
        List<StockTakingTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getOrderId, orderId)
                .orderByAsc(StockTakingTask::getLocationCode).orderByAsc(StockTakingTask::getId));
        Map<Long, Material> materialMap = materialMap();
        Map<String, Location> locationMap = locationMap();
        return tasks.stream().map(t -> taskRow(t, materialMap.get(t.getMaterialId()), locationMap.get(t.getLocationCode()))).toList();
    }

    private Map<String, Object> orderRow(StockTakingOrder order, Map<String, Warehouse> warehouseMap) {
        Map<String, Object> result = new HashMap<>();
        Warehouse warehouse = warehouseMap.get(order.getWarehouseCode());
        result.put("id", order.getId());
        result.put("code", order.getCode());
        result.put("warehouseCode", order.getWarehouseCode());
        result.put("warehouse", warehouse != null ? warehouse.getName() : order.getWarehouseCode());
        result.put("scopeDesc", order.getScopeDesc());
        result.put("type", order.getType());
        result.put("taskCount", nzi(order.getTaskCount()));
        result.put("completedCount", nzi(order.getCompletedCount()));
        result.put("diffCount", nzi(order.getDiffCount()));
        result.put("status", order.getStatus());
        result.put("creator", order.getCreator());
        result.put("remark", order.getRemark());
        result.put("createTime", order.getCreateTime());
        return result;
    }

    private Map<String, Object> taskRow(StockTakingTask task, Material material, Location location) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", task.getId());
        result.put("inventoryId", task.getInventoryId());
        result.put("materialId", task.getMaterialId());
        result.put("materialCode", material == null ? null : material.getSku());
        result.put("materialName", material == null ? null : material.getName());
        result.put("spec", material == null ? null : material.getSpec());
        result.put("unit", material == null ? null : material.getUnit());
        result.put("locationCode", task.getLocationCode());
        result.put("location", location != null ? location.getName() : task.getLocationCode());
        result.put("batchNo", task.getBatchNo());
        result.put("bookQty", nz(task.getBookQty()));
        result.put("actualQty", nz(task.getActualQty()));
        result.put("diffQty", nz(task.getDiffQty()));
        result.put("diffAmount", nz(task.getDiffAmount()));
        result.put("hasDiff", task.getHasDiff() != null && task.getHasDiff() == 1);
        result.put("unitPrice", nz(task.getUnitPrice()));
        result.put("status", task.getStatus());
        result.put("reviewStatus", task.getReviewStatus());
        result.put("remark", task.getRemark());
        return result;
    }

    private String scopeDescOf(String warehouseCode, List<String> locationCodes) {
        if (locationCodes == null || locationCodes.isEmpty()) return "全仓库";
        return "库位：" + String.join("、", locationCodes);
    }

    private List<String> parseLocationCodes(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(this::text).filter(StringUtils::hasText).distinct().toList();
        }
        String text = text(value);
        if (!StringUtils.hasText(text)) return List.of();
        return Arrays.stream(text.split("[,，\\n]")).map(String::trim).filter(StringUtils::hasText).distinct().toList();
    }

    private String generateCode() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("PD%04d%02d%02d%02d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond());
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

    private Long longValue(Object value, String message) {
        if (value instanceof Number number) return number.longValue();
        String text = text(value);
        if (StringUtils.hasText(text)) {
            try { return Long.parseLong(text); } catch (NumberFormatException ignored) { }
        }
        throw new IllegalArgumentException(message);
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

    private int nzi(Integer value) {
        return value == null ? 0 : value;
    }
}
