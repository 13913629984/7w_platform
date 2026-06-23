package com.qws.wms;

import com.qws.common.entity.Material;
import com.qws.common.entity.OutboundItem;
import com.qws.common.entity.OutboundOrder;
import com.qws.common.entity.OutboundRecord;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.OutboundItemMapper;
import com.qws.common.mapper.OutboundOrderMapper;
import com.qws.common.mapper.OutboundRecordMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OutboundDataInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final OutboundOrderMapper orderMapper;
    private final OutboundItemMapper itemMapper;
    private final OutboundRecordMapper recordMapper;
    private final MaterialMapper materialMapper;

    public OutboundDataInitializer(JdbcTemplate jdbcTemplate, OutboundOrderMapper orderMapper, OutboundItemMapper itemMapper,
                                   OutboundRecordMapper recordMapper, MaterialMapper materialMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.recordMapper = recordMapper;
        this.materialMapper = materialMapper;
    }

    @PostConstruct
    public void init() {
        createTables();
        seed();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_outbound_order (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    order_no VARCHAR(80),
                    customer VARCHAR(120),
                    warehouse_code VARCHAR(50) NOT NULL,
                    material_count INT NOT NULL DEFAULT 0,
                    total_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    outbound_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                    status VARCHAR(30) NOT NULL DEFAULT 'pending',
                    creator VARCHAR(60),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_outbound_order_code (code),
                    INDEX idx_warehouse (warehouse_code),
                    INDEX idx_status (status),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_outbound_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    material_id BIGINT NOT NULL,
                    location_code VARCHAR(50),
                    batch_no VARCHAR(80),
                    method VARCHAR(20) NOT NULL DEFAULT 'qty',
                    quantity DECIMAL(14,3) NOT NULL DEFAULT 0,
                    outbound_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    unit_price DECIMAL(14,2) NOT NULL DEFAULT 0,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_order (order_id),
                    INDEX idx_material (material_id),
                    INDEX idx_location (location_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_outbound_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    item_id BIGINT NOT NULL,
                    material_id BIGINT NOT NULL,
                    location_code VARCHAR(50) NOT NULL,
                    batch_no VARCHAR(80),
                    method VARCHAR(20) NOT NULL DEFAULT 'qty',
                    quantity DECIMAL(14,3) NOT NULL DEFAULT 0,
                    unit_price DECIMAL(14,2) NOT NULL DEFAULT 0,
                    operator VARCHAR(60),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_order (order_id),
                    INDEX idx_item (item_id),
                    INDEX idx_material (material_id),
                    INDEX idx_location (location_code),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        if (orderMapper.selectCount(null) > 0) {
            return;
        }
        Map<String, Long> materialIds = loadMaterialIds();

        Long order1 = insertOrder("OUT202606001", "SO202606001", "深圳云测科技有限公司", "WH-A", 2, bd(50), bd(50), bd(34900), "completed", "admin", "电子件销售出库");
        Long item11 = insertItem(order1, materialIds.get("MAT-EL-002"), "A-01-002", "qty", bd(30), bd(30), bd(480), "通信模块出库");
        Long item12 = insertItem(order1, materialIds.get("MAT-EL-003"), "A-02-001", "qty", bd(20), bd(20), bd(220), "电源模块出库");
        insertRecord(order1, item11, materialIds.get("MAT-EL-002"), "A-01-002", "qty", bd(30), bd(480), "admin");
        insertRecord(order1, item12, materialIds.get("MAT-EL-003"), "A-02-001", "qty", bd(20), bd(220), "admin");

        Long order2 = insertOrder("OUT202606002", "SO202606002", "宁波自动化集成商", "WH-B", 2, bd(400), bd(180), bd(8540), "partial", "admin", "五金件部分出库");
        Long item21 = insertItem(order2, materialIds.get("MAT-HW-001"), "B-01-001", "qty", bd(200), bd(120), bd(86), "螺栓部分出库");
        Long item22 = insertItem(order2, materialIds.get("MAT-RB-001"), "B-01-001", "qty", bd(200), bd(60), bd(3.5), "密封圈部分出库");
        insertRecord(order2, item21, materialIds.get("MAT-HW-001"), "B-01-001", "qty", bd(120), bd(86), "admin");
        insertRecord(order2, item22, materialIds.get("MAT-RB-001"), "B-01-001", "qty", bd(60), bd(3.5), "admin");

        Long order3 = insertOrder("OUT202606003", "SO202606003", "成都工控方案商", "WH-FG", 1, bd(10), bd(0), bd(186000), "pending", "admin", "成品控制柜待出库");
        insertItem(order3, materialIds.get("MAT-FG-001"), "FG-01-001", "qty", bd(10), bd(0), bd(18600), "控制柜待出库");
    }

    private Map<String, Long> loadMaterialIds() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getSku, Material::getId, (a, b) -> a));
    }

    private Long insertOrder(String code, String orderNo, String customer, String warehouseCode, int materialCount,
                             BigDecimal totalQty, BigDecimal outboundQty, BigDecimal totalAmount, String status,
                             String creator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        OutboundOrder order = new OutboundOrder();
        order.setCode(code);
        order.setOrderNo(orderNo);
        order.setCustomer(customer);
        order.setWarehouseCode(warehouseCode);
        order.setMaterialCount(materialCount);
        order.setTotalQty(totalQty);
        order.setOutboundQty(outboundQty);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setCreator(creator);
        order.setRemark(remark);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);
        return order.getId();
    }

    private Long insertItem(Long orderId, Long materialId, String locationCode, String method,
                            BigDecimal quantity, BigDecimal outboundQty, BigDecimal unitPrice, String remark) {
        if (materialId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        OutboundItem item = new OutboundItem();
        item.setOrderId(orderId);
        item.setMaterialId(materialId);
        item.setLocationCode(locationCode);
        item.setBatchNo(null);
        item.setMethod(method);
        item.setQuantity(quantity);
        item.setOutboundQty(outboundQty);
        item.setUnitPrice(unitPrice);
        item.setRemark(remark);
        item.setCreateTime(now);
        item.setUpdateTime(now);
        itemMapper.insert(item);
        return item.getId();
    }

    private void insertRecord(Long orderId, Long itemId, Long materialId, String locationCode,
                              String method, BigDecimal quantity, BigDecimal unitPrice, String operator) {
        if (itemId == null || materialId == null) {
            return;
        }
        OutboundRecord record = new OutboundRecord();
        record.setOrderId(orderId);
        record.setItemId(itemId);
        record.setMaterialId(materialId);
        record.setLocationCode(locationCode);
        record.setBatchNo(null);
        record.setMethod(method);
        record.setQuantity(quantity);
        record.setUnitPrice(unitPrice);
        record.setOperator(operator);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);
    }

    private BigDecimal bd(Object value) {
        return new BigDecimal(String.valueOf(value));
    }
}
