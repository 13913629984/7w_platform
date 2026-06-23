package com.qws.wms;

import com.qws.common.entity.Inventory;
import com.qws.common.entity.Material;
import com.qws.common.entity.StockTakingOrder;
import com.qws.common.entity.StockTakingTask;
import com.qws.common.mapper.InventoryMapper;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.StockTakingOrderMapper;
import com.qws.common.mapper.StockTakingTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@DependsOn("inventoryDataInitializer")
public class StocktakingDataInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final StockTakingOrderMapper orderMapper;
    private final StockTakingTaskMapper taskMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;

    public StocktakingDataInitializer(JdbcTemplate jdbcTemplate, StockTakingOrderMapper orderMapper,
                                      StockTakingTaskMapper taskMapper, InventoryMapper inventoryMapper,
                                      MaterialMapper materialMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderMapper = orderMapper;
        this.taskMapper = taskMapper;
        this.inventoryMapper = inventoryMapper;
        this.materialMapper = materialMapper;
    }

    @PostConstruct
    public void init() {
        createTables();
        seed();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_stocktaking_order (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    warehouse_code VARCHAR(50) NOT NULL,
                    scope_desc VARCHAR(255),
                    type VARCHAR(20) NOT NULL DEFAULT 'full',
                    task_count INT NOT NULL DEFAULT 0,
                    completed_count INT NOT NULL DEFAULT 0,
                    diff_count INT NOT NULL DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'pending',
                    creator VARCHAR(60),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_stocktaking_order_code (code),
                    INDEX idx_warehouse (warehouse_code),
                    INDEX idx_status (status),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_stocktaking_task (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    inventory_id BIGINT,
                    material_id BIGINT NOT NULL,
                    location_code VARCHAR(50) NOT NULL,
                    batch_no VARCHAR(80),
                    book_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    actual_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    diff_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    diff_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                    has_diff TINYINT NOT NULL DEFAULT 0,
                    unit_price DECIMAL(14,2) NOT NULL DEFAULT 0,
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    review_status VARCHAR(20),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_order (order_id),
                    INDEX idx_material (material_id),
                    INDEX idx_location (location_code),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        if (orderMapper.selectCount(null) > 0) {
            return;
        }
        Map<Long, Material> materialMap = materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity(), (a, b) -> a));

        // 已完成盘点单（WH-A 全盘，含已确认差异）
        Long order1 = insertOrder("PD202606A001", "WH-A", "全仓库", "full", "completed", "admin", "A区全盘已完成");
        seedTasks(order1, "WH-A", null, materialMap, true, true);

        // 待审核盘点单（WH-B 循环盘点，差异待审核）
        Long order2 = insertOrder("PD202606B001", "WH-B", "全仓库", "cycle", "pending_review", "admin", "B区循环盘点待审核");
        seedTasks(order2, "WH-B", null, materialMap, true, false);

        // 进行中盘点单（WH-FG 抽盘，部分完成）
        Long order3 = insertOrder("PD202606F001", "WH-FG", "全仓库", "spot", "processing", "admin", "成品仓抽盘进行中");
        seedTasks(order3, "WH-FG", null, materialMap, false, false);

        // 待开始盘点单（WH-SP，任务在启动时生成）
        insertOrder("PD202606S001", "WH-SP", "全仓库", "full", "pending", "admin", "备件仓待开始盘点");

        refreshCounts(order1);
        refreshCounts(order2);
        refreshCounts(order3);
    }

    /**
     * @param completeAll true 则所有任务标记为已完成；false 则仅首条完成
     * @param approved    差异任务的审核状态是否为 approved（true）或 pending（false）
     */
    private void seedTasks(Long orderId, String warehouseCode, List<String> locationCodes,
                           Map<Long, Material> materialMap, boolean completeAll, boolean approved) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, warehouseCode);
        if (locationCodes != null && !locationCodes.isEmpty()) wrapper.in(Inventory::getLocationCode, locationCodes);
        wrapper.orderByAsc(Inventory::getLocationCode).orderByAsc(Inventory::getId);
        List<Inventory> inventories = inventoryMapper.selectList(wrapper);

        int index = 0;
        for (Inventory inventory : inventories) {
            Material material = materialMap.get(inventory.getMaterialId());
            BigDecimal unitPrice = material != null && material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal bookQty = nz(inventory.getQuantity());
            boolean completed = completeAll || index == 0;
            // 每隔一条制造一个差异，便于演示差异审核流程
            boolean makeDiff = completed && index % 2 == 1;
            BigDecimal actualQty = completed ? (makeDiff ? bookQty.subtract(BigDecimal.valueOf(2)).max(BigDecimal.ZERO) : bookQty) : BigDecimal.ZERO;
            BigDecimal diffQty = completed ? actualQty.subtract(bookQty) : BigDecimal.ZERO;
            boolean hasDiff = diffQty.signum() != 0;

            StockTakingTask task = new StockTakingTask();
            task.setOrderId(orderId);
            task.setInventoryId(inventory.getId());
            task.setMaterialId(inventory.getMaterialId());
            task.setLocationCode(inventory.getLocationCode());
            task.setBatchNo(inventory.getBatchNo());
            task.setBookQty(bookQty);
            task.setActualQty(actualQty);
            task.setDiffQty(diffQty);
            task.setDiffAmount(diffQty.multiply(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP));
            task.setHasDiff(hasDiff ? 1 : 0);
            task.setUnitPrice(unitPrice);
            task.setStatus(completed ? "completed" : "pending");
            task.setReviewStatus(hasDiff ? (approved ? "approved" : "pending") : "");
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.insert(task);
            index++;
        }
    }

    private void refreshCounts(Long orderId) {
        List<StockTakingTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<StockTakingTask>()
                .eq(StockTakingTask::getOrderId, orderId));
        StockTakingOrder order = orderMapper.selectById(orderId);
        if (order == null) return;
        order.setTaskCount(tasks.size());
        order.setCompletedCount((int) tasks.stream().filter(t -> "completed".equals(t.getStatus())).count());
        order.setDiffCount((int) tasks.stream().filter(t -> t.getHasDiff() != null && t.getHasDiff() == 1).count());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private Long insertOrder(String code, String warehouseCode, String scopeDesc, String type,
                             String status, String creator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        StockTakingOrder order = new StockTakingOrder();
        order.setCode(code);
        order.setWarehouseCode(warehouseCode);
        order.setScopeDesc(scopeDesc);
        order.setType(type);
        order.setStatus(status);
        order.setCreator(creator);
        order.setRemark(remark);
        order.setTaskCount(0);
        order.setCompletedCount(0);
        order.setDiffCount(0);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);
        return order.getId();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
