package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.BomNode;
import com.qws.common.entity.BomNodeMaterial;
import com.qws.common.entity.BomProduct;
import com.qws.common.entity.Material;
import com.qws.common.mapper.BomNodeMapper;
import com.qws.common.mapper.BomNodeMaterialMapper;
import com.qws.common.mapper.BomProductMapper;
import com.qws.common.mapper.MaterialMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BomDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final BomProductMapper productMapper;
    private final BomNodeMapper nodeMapper;
    private final BomNodeMaterialMapper nodeMaterialMapper;
    private final MaterialMapper materialMapper;

    public BomDataInitializer(JdbcTemplate jdbcTemplate, BomProductMapper productMapper,
                             BomNodeMapper nodeMapper, BomNodeMaterialMapper nodeMaterialMapper,
                             MaterialMapper materialMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.productMapper = productMapper;
        this.nodeMapper = nodeMapper;
        this.nodeMaterialMapper = nodeMaterialMapper;
        this.materialMapper = materialMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        seed();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_bom_product (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(100) NOT NULL,
                    spec VARCHAR(100),
                    category VARCHAR(50),
                    status TINYINT DEFAULT 1,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_bom_product_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_bom_node (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    product_id BIGINT NOT NULL,
                    parent_id BIGINT,
                    node_name VARCHAR(100) NOT NULL,
                    level INT DEFAULT 1,
                    sort INT DEFAULT 0,
                    remark VARCHAR(500),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_bom_node_product (product_id),
                    INDEX idx_bom_node_parent (parent_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wms_bom_node_material (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    node_id BIGINT NOT NULL,
                    material_id BIGINT NOT NULL,
                    material_type VARCHAR(20) NOT NULL,
                    quantity DECIMAL(12,2) DEFAULT 1,
                    sort INT DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_bom_node_material_node (node_id),
                    INDEX idx_bom_node_material_material (material_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seed() {
        Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_bom_product", Integer.class);
        Integer materialCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wms_bom_node_material", Integer.class);
        if (productCount != null && productCount > 0 && materialCount != null && materialCount > 0) {
            // 已完整初始化，跳过
            return;
        }
        // 清理可能的不完整种子数据（节点已建但物料缺失）后重新初始化
        if (productCount != null && productCount > 0) {
            jdbcTemplate.execute("DELETE FROM wms_bom_node_material");
            jdbcTemplate.execute("DELETE FROM wms_bom_node");
            jdbcTemplate.execute("DELETE FROM wms_bom_product");
        }

        BomProduct product = new BomProduct();
        product.setCode("PROD-QW900");
        product.setName("智能控制柜");
        product.setSpec("QW-900");
        product.setCategory("控制柜");
        product.setStatus(1);
        product.setRemark("标准智能控制柜产品BOM示例");
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        Long pid = product.getId();

        Long root = addNode(pid, null, "整机装配", 1, 1, "产品总装配节点");
        Long n1 = addNode(pid, root, "主板模块", 2, 1, "主控板及通信");
        Long n2 = addNode(pid, root, "电源模块", 2, 2, "供电单元");
        Long n3 = addNode(pid, root, "结构件", 2, 3, "机箱及紧固件");
        Long n4 = addNode(pid, root, "密封组件", 2, 4, "密封与减震");

        // 使用当前库中已有的物料进行示例挂载，避免硬编码不存在的物料编码
        List<Material> mats = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().orderByAsc(Material::getId));
        if (!mats.isEmpty()) {
            int n = Math.min(mats.size(), 8);
            assignMaterial(n1, mats.get(0).getId(), "MAIN", new BigDecimal("1"));
            assignMaterial(n2, mats.get(1 % n).getId(), "MAIN", new BigDecimal("1"));
            assignMaterial(n2, mats.get(2 % n).getId(), "BACKUP", new BigDecimal("1"));
            assignMaterial(n3, mats.get(3 % n).getId(), "MAIN", new BigDecimal("1"));
            assignMaterial(n3, mats.get(4 % n).getId(), "BACKUP", new BigDecimal("12"));
            assignMaterial(n4, mats.get(5 % n).getId(), "MAIN", new BigDecimal("4"));
            assignMaterial(n4, mats.get(6 % n).getId(), "BACKUP", new BigDecimal("4"));
        }
    }

    private Long addNode(Long productId, Long parentId, String nodeName, int level, int sort, String remark) {
        BomNode node = new BomNode();
        node.setProductId(productId);
        node.setParentId(parentId);
        node.setNodeName(nodeName);
        node.setLevel(level);
        node.setSort(sort);
        node.setRemark(remark);
        node.setCreateTime(LocalDateTime.now());
        node.setUpdateTime(LocalDateTime.now());
        nodeMapper.insert(node);
        return node.getId();
    }

    private void assignMaterial(Long nodeId, Long materialId, String type, BigDecimal quantity) {
        BomNodeMaterial entity = new BomNodeMaterial();
        entity.setNodeId(nodeId);
        entity.setMaterialId(materialId);
        entity.setMaterialType(type);
        entity.setQuantity(quantity);
        entity.setSort(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        nodeMaterialMapper.insert(entity);
    }
}
