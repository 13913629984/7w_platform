package com.qws.wms;

import com.qws.common.entity.InboundItem;
import com.qws.common.entity.InboundOrder;
import com.qws.common.entity.InboundRecord;
import com.qws.common.entity.InboundSn;
import com.qws.common.entity.Material;
import com.qws.common.mapper.InboundItemMapper;
import com.qws.common.mapper.InboundOrderMapper;
import com.qws.common.mapper.InboundRecordMapper;
import com.qws.common.mapper.InboundSnMapper;
import com.qws.common.mapper.MaterialMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InboundDataInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final InboundOrderMapper orderMapper;
    private final InboundItemMapper itemMapper;
    private final InboundRecordMapper recordMapper;
    private final InboundSnMapper snMapper;
    private final MaterialMapper materialMapper;

    public InboundDataInitializer(JdbcTemplate jdbcTemplate, InboundOrderMapper orderMapper, InboundItemMapper itemMapper,
                                  InboundRecordMapper recordMapper, InboundSnMapper snMapper, MaterialMapper materialMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.recordMapper = recordMapper;
        this.snMapper = snMapper;
        this.materialMapper = materialMapper;
    }

    @PostConstruct
    public void init() {
        createTables();
        seed();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_inbound_order (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    po_code VARCHAR(80),
                    supplier VARCHAR(120),
                    warehouse_code VARCHAR(50) NOT NULL,
                    material_count INT NOT NULL DEFAULT 0,
                    total_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    inbound_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                    inspect_status VARCHAR(30) NOT NULL DEFAULT 'passed',
                    status VARCHAR(30) NOT NULL DEFAULT 'pending',
                    creator VARCHAR(60),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_inbound_order_code (code),
                    INDEX idx_warehouse (warehouse_code),
                    INDEX idx_status (status),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_inbound_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    material_id BIGINT NOT NULL,
                    location_code VARCHAR(50) NOT NULL,
                    batch_no VARCHAR(80),
                    method VARCHAR(20) NOT NULL DEFAULT 'qty',
                    quantity DECIMAL(14,3) NOT NULL DEFAULT 0,
                    inbound_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    unit_price DECIMAL(14,2) NOT NULL DEFAULT 0,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_order (order_id),
                    INDEX idx_material (material_id),
                    INDEX idx_location (location_code),
                    INDEX idx_batch (batch_no)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_inbound_sn (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    item_id BIGINT NOT NULL,
                    material_id BIGINT NOT NULL,
                    location_code VARCHAR(50) NOT NULL,
                    sn VARCHAR(120) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'inbound',
                    inbound_time DATETIME,
                    outbound_code VARCHAR(80),
                    outbound_time DATETIME,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_inbound_sn (sn),
                    INDEX idx_order (order_id),
                    INDEX idx_item (item_id),
                    INDEX idx_material (material_id),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_inbound_record (
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
        Long order1 = insertOrder("IN202606001", "PO202606001", "上海智造电子有限公司", "WH-A", 2, bd(80), bd(80), bd(45600), "passed", "completed", "admin", "电子件到货入库");
        Long item11 = insertItem(order1, materialIds.get("MAT-EL-001"), "A-01-001", "INB202606001", "sn", bd(20), bd(20), bd(980), "主板 SN 入库");
        Long item12 = insertItem(order1, materialIds.get("MAT-EL-002"), "A-01-002", "INB202606001", "qty", bd(60), bd(60), bd(430), "通信模块数量入库");
        insertRecord(order1, item11, materialIds.get("MAT-EL-001"), "A-01-001", "INB202606001", "sn", bd(20), bd(980), "admin");
        insertRecord(order1, item12, materialIds.get("MAT-EL-002"), "A-01-002", "INB202606001", "qty", bd(60), bd(430), "admin");
        for (int i = 1; i <= 5; i++) {
            insertSn(order1, item11, materialIds.get("MAT-EL-001"), "A-01-001", "SN-IN-2606001-" + String.format("%03d", i));
        }

        Long order2 = insertOrder("IN202606002", "PO202606002", "苏州五金制造厂", "WH-B", 2, bd(500), bd(260), bd(18600), "passed", "partial", "admin", "五金件部分入库");
        Long item21 = insertItem(order2, materialIds.get("MAT-HW-001"), "B-01-001", "INB202606002", "qty", bd(300), bd(180), bd(22), "螺栓部分入库");
        Long item22 = insertItem(order2, materialIds.get("MAT-HW-002"), "B-01-001", "INB202606002", "qty", bd(200), bd(80), bd(60), "垫片部分入库");
        insertRecord(order2, item21, materialIds.get("MAT-HW-001"), "B-01-001", "INB202606002", "qty", bd(180), bd(22), "admin");
        insertRecord(order2, item22, materialIds.get("MAT-HW-002"), "B-01-001", "INB202606002", "qty", bd(80), bd(60), "admin");

        Long order3 = insertOrder("IN202606003", "PO202606003", "杭州橡塑科技有限公司", "WH-SP", 1, bd(120), bd(0), bd(7800), "pending", "pending", "admin", "待质检入库");
        insertItem(order3, materialIds.get("MAT-RB-002"), "SP-01-001", "INB202606003", "qty", bd(120), bd(0), bd(65), "减震垫待入库");
    }

    private Map<String, Long> loadMaterialIds() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getSku, Material::getId, (a, b) -> a));
    }

    private Long insertOrder(String code, String poCode, String supplier, String warehouseCode, int materialCount,
                             BigDecimal totalQty, BigDecimal inboundQty, BigDecimal totalAmount, String inspectStatus,
                             String status, String creator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        InboundOrder order = new InboundOrder();
        order.setCode(code);
        order.setPoCode(poCode);
        order.setSupplier(supplier);
        order.setWarehouseCode(warehouseCode);
        order.setMaterialCount(materialCount);
        order.setTotalQty(totalQty);
        order.setInboundQty(inboundQty);
        order.setTotalAmount(totalAmount);
        order.setInspectStatus(inspectStatus);
        order.setStatus(status);
        order.setCreator(creator);
        order.setRemark(remark);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);
        return order.getId();
    }

    private Long insertItem(Long orderId, Long materialId, String locationCode, String batchNo, String method,
                            BigDecimal quantity, BigDecimal inboundQty, BigDecimal unitPrice, String remark) {
        if (materialId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        InboundItem item = new InboundItem();
        item.setOrderId(orderId);
        item.setMaterialId(materialId);
        item.setLocationCode(locationCode);
        item.setBatchNo(batchNo);
        item.setMethod(method);
        item.setQuantity(quantity);
        item.setInboundQty(inboundQty);
        item.setUnitPrice(unitPrice);
        item.setRemark(remark);
        item.setCreateTime(now);
        item.setUpdateTime(now);
        itemMapper.insert(item);
        return item.getId();
    }

    private void insertSn(Long orderId, Long itemId, Long materialId, String locationCode, String sn) {
        if (itemId == null || materialId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        InboundSn entity = new InboundSn();
        entity.setOrderId(orderId);
        entity.setItemId(itemId);
        entity.setMaterialId(materialId);
        entity.setLocationCode(locationCode);
        entity.setSn(sn);
        entity.setStatus("inbound");
        entity.setInboundTime(now);
        entity.setCreateTime(now);
        snMapper.insert(entity);
    }

    private void insertRecord(Long orderId, Long itemId, Long materialId, String locationCode, String batchNo,
                              String method, BigDecimal quantity, BigDecimal unitPrice, String operator) {
        if (itemId == null || materialId == null) {
            return;
        }
        InboundRecord record = new InboundRecord();
        record.setOrderId(orderId);
        record.setItemId(itemId);
        record.setMaterialId(materialId);
        record.setLocationCode(locationCode);
        record.setBatchNo(batchNo);
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
