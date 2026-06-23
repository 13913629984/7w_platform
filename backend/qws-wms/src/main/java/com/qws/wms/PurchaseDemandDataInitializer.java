package com.qws.wms;

import com.qws.common.entity.Material;
import com.qws.common.entity.PurchaseDemand;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.PurchaseDemandMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PurchaseDemandDataInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final PurchaseDemandMapper demandMapper;
    private final MaterialMapper materialMapper;

    public PurchaseDemandDataInitializer(JdbcTemplate jdbcTemplate, PurchaseDemandMapper demandMapper, MaterialMapper materialMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.demandMapper = demandMapper;
        this.materialMapper = materialMapper;
    }

    @PostConstruct
    public void init() {
        createTable();
        seed();
    }

    private void createTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_purchase_demand (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    source VARCHAR(20) NOT NULL DEFAULT 'manual',
                    material_id BIGINT,
                    sku VARCHAR(50) NOT NULL,
                    material_name VARCHAR(120) NOT NULL,
                    spec VARCHAR(120),
                    warehouse_code VARCHAR(50),
                    qty DECIMAL(14,3) NOT NULL DEFAULT 0,
                    estimated_price DECIMAL(14,2) NOT NULL DEFAULT 0,
                    demand_date DATE,
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    po_code VARCHAR(80),
                    requester VARCHAR(60),
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_demand_code (code),
                    INDEX idx_source (source),
                    INDEX idx_status (status),
                    INDEX idx_sku (sku),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        if (demandMapper.selectCount(null) > 0) {
            return;
        }
        Map<String, Long> materialIds = loadMaterialIds();
        insert("PR202606001", "warning", materialIds.get("MAT-RB-002"), "MAT-RB-002", "减震垫", "60*60*10", "WH-SP",
                bd(400), bd(8), LocalDate.now().plusDays(7), "pending", null, "admin", "安全库存预警自动生成");
        insert("PR202606002", "mrp", materialIds.get("MAT-EL-003"), "MAT-EL-003", "电源模块", "DR-24V-5A", "WH-A",
                bd(120), bd(220), LocalDate.now().plusDays(10), "ordered", "PO202606001", "admin", "MRP计算需求");
        insert("PR202606003", "manual", materialIds.get("MAT-HW-002"), "MAT-HW-002", "镀锌垫片", "M8", "WH-B",
                bd(500), bd(22), LocalDate.now().plusDays(5), "completed", "PO202605008", "admin", "手动补货需求");
    }

    private Map<String, Long> loadMaterialIds() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(Material::getSku, Material::getId, (a, b) -> a));
    }

    private void insert(String code, String source, Long materialId, String sku, String name, String spec,
                        String warehouseCode, BigDecimal qty, BigDecimal price, LocalDate demandDate,
                        String status, String poCode, String requester, String remark) {
        LocalDateTime now = LocalDateTime.now();
        PurchaseDemand demand = new PurchaseDemand();
        demand.setCode(code);
        demand.setSource(source);
        demand.setMaterialId(materialId);
        demand.setSku(sku);
        demand.setMaterialName(name);
        demand.setSpec(spec);
        demand.setWarehouseCode(warehouseCode);
        demand.setQty(qty);
        demand.setEstimatedPrice(price);
        demand.setDemandDate(demandDate);
        demand.setStatus(status);
        demand.setPoCode(poCode);
        demand.setRequester(requester);
        demand.setRemark(remark);
        demand.setCreateTime(now);
        demand.setUpdateTime(now);
        demandMapper.insert(demand);
    }

    private BigDecimal bd(Object value) {
        return new BigDecimal(String.valueOf(value));
    }
}
