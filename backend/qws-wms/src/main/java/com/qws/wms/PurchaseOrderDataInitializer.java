package com.qws.wms;

import com.qws.common.entity.Material;
import com.qws.common.entity.PurchaseOrder;
import com.qws.common.entity.PurchaseOrderItem;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.PurchaseOrderItemMapper;
import com.qws.common.mapper.PurchaseOrderMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PurchaseOrderDataInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final MaterialMapper materialMapper;

    public PurchaseOrderDataInitializer(JdbcTemplate jdbcTemplate, PurchaseOrderMapper orderMapper,
                                        PurchaseOrderItemMapper itemMapper, MaterialMapper materialMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.materialMapper = materialMapper;
    }

    @PostConstruct
    public void init() {
        createTables();
        seed();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_purchase_order (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    supplier VARCHAR(120) NOT NULL,
                    demand_codes VARCHAR(500),
                    warehouse_code VARCHAR(50),
                    material_count INT NOT NULL DEFAULT 0,
                    total_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                    delivery_date DATE,
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    inbound_code VARCHAR(80),
                    creator VARCHAR(60),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_po_code (code),
                    INDEX idx_supplier (supplier),
                    INDEX idx_status (status),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_purchase_order_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    material_id BIGINT,
                    sku VARCHAR(50) NOT NULL,
                    material_name VARCHAR(120) NOT NULL,
                    spec VARCHAR(120),
                    qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    unit_price DECIMAL(14,2) NOT NULL DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_order (order_id),
                    INDEX idx_sku (sku)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        if (orderMapper.selectCount(null) > 0) {
            return;
        }
        Map<String, Long> materialIds = loadMaterialIds();

        Long order1 = insertOrder("PO202606001", "上海智造电子有限公司", "PR202606002", "WH-A", 2,
                bd(140), bd(31400), LocalDate.now().plusDays(10), "confirmed", null, "admin", "电子件采购");
        insertItem(order1, materialIds.get("MAT-EL-003"), "MAT-EL-003", "电源模块", "DR-24V-5A", bd(120), bd(220));
        insertItem(order1, materialIds.get("MAT-EL-002"), "MAT-EL-002", "通信模块", "RS485", bd(20), bd(480));

        Long order2 = insertOrder("PO202606002", "苏州五金制造厂", "", "WH-B", 1,
                bd(500), bd(11000), LocalDate.now().plusDays(7), "pending", null, "admin", "五金件采购");
        insertItem(order2, materialIds.get("MAT-HW-002"), "MAT-HW-002", "镀锌垫片", "M8", bd(500), bd(22));

        Long order3 = insertOrder("PO202605008", "杭州橡塑科技有限公司", "PR202606003", "WH-SP", 1,
                bd(500), bd(4000), LocalDate.now().minusDays(3), "inbound", "IN-PO202605008", "admin", "减震垫采购已入库");
        insertItem(order3, materialIds.get("MAT-RB-002"), "MAT-RB-002", "减震垫", "60*60*10", bd(500), bd(8));
    }

    private Map<String, Long> loadMaterialIds() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getSku, Material::getId, (a, b) -> a));
    }

    private Long insertOrder(String code, String supplier, String demandCodes, String warehouseCode, int materialCount,
                             BigDecimal totalQty, BigDecimal totalAmount, LocalDate deliveryDate, String status,
                             String inboundCode, String creator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        PurchaseOrder order = new PurchaseOrder();
        order.setCode(code);
        order.setSupplier(supplier);
        order.setDemandCodes(demandCodes);
        order.setWarehouseCode(warehouseCode);
        order.setMaterialCount(materialCount);
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setDeliveryDate(deliveryDate);
        order.setStatus(status);
        order.setInboundCode(inboundCode);
        order.setCreator(creator);
        order.setRemark(remark);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);
        return order.getId();
    }

    private void insertItem(Long orderId, Long materialId, String sku, String name, String spec, BigDecimal qty, BigDecimal unitPrice) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setOrderId(orderId);
        item.setMaterialId(materialId);
        item.setSku(sku);
        item.setMaterialName(name);
        item.setSpec(spec);
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        item.setCreateTime(LocalDateTime.now());
        itemMapper.insert(item);
    }

    private BigDecimal bd(Object value) {
        return new BigDecimal(String.valueOf(value));
    }
}
