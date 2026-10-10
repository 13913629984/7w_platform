package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.bom.BomNodeMaterialRequest;
import com.qws.common.dto.bom.BomNodeMaterialVO;
import com.qws.common.dto.bom.BomNodeRequest;
import com.qws.common.dto.bom.BomNodeVO;
import com.qws.common.dto.bom.BomProductListResult;
import com.qws.common.dto.bom.BomProductRequest;
import com.qws.common.dto.bom.BomProductVO;
import com.qws.common.entity.BomNode;
import com.qws.common.entity.BomNodeMaterial;
import com.qws.common.entity.BomProduct;
import com.qws.common.entity.Material;
import com.qws.common.mapper.BomNodeMapper;
import com.qws.common.mapper.BomNodeMaterialMapper;
import com.qws.common.mapper.BomProductMapper;
import com.qws.common.mapper.MaterialMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BomService {

    private final BomProductMapper productMapper;
    private final BomNodeMapper nodeMapper;
    private final BomNodeMaterialMapper nodeMaterialMapper;
    private final MaterialMapper materialMapper;

    public BomService(BomProductMapper productMapper, BomNodeMapper nodeMapper,
                      BomNodeMaterialMapper nodeMaterialMapper, MaterialMapper materialMapper) {
        this.productMapper = productMapper;
        this.nodeMapper = nodeMapper;
        this.nodeMaterialMapper = nodeMaterialMapper;
        this.materialMapper = materialMapper;
    }

    public BomProductListResult listProducts(String keyword, Integer status, int page, int pageSize) {
        LambdaQueryWrapper<BomProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BomProduct::getCode, keyword)
                    .or().like(BomProduct::getName, keyword)
                    .or().like(BomProduct::getSpec, keyword));
        }
        if (status != null) {
            wrapper.eq(BomProduct::getStatus, status);
        }
        wrapper.orderByDesc(BomProduct::getUpdateTime);
        long total = productMapper.selectCount(wrapper);
        List<BomProduct> records = productMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, pageSize), wrapper)
                .getRecords();
        List<BomProductVO> rows = records.stream().map(this::toProductVO).collect(Collectors.toList());
        BomProductListResult result = new BomProductListResult();
        result.setRows(rows);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setStats(computeStats());
        return result;
    }

    public BomProductVO getProduct(Long id) {
        BomProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }
        return toProductVO(product);
    }

    @Transactional
    public BomProductVO createProduct(BomProductRequest request) {
        validateProduct(request);
        BomProduct exists = productMapper.selectOne(new LambdaQueryWrapper<BomProduct>()
                .eq(BomProduct::getCode, request.getCode()));
        if (exists != null) {
            throw new IllegalStateException("产品编码已存在");
        }
        BomProduct product = new BomProduct();
        applyProduct(product, request);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("产品编码已存在");
        }
        return toProductVO(product);
    }

    @Transactional
    public BomProductVO updateProduct(BomProductRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的产品");
        }
        validateProduct(request);
        BomProduct product = productMapper.selectById(request.getId());
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }
        if (!product.getCode().equals(request.getCode())) {
            BomProduct conflict = productMapper.selectOne(new LambdaQueryWrapper<BomProduct>()
                    .eq(BomProduct::getCode, request.getCode()).ne(BomProduct::getId, request.getId()));
            if (conflict != null) {
                throw new IllegalStateException("产品编码已存在");
            }
        }
        applyProduct(product, request);
        product.setUpdateTime(LocalDateTime.now());
        try {
            productMapper.updateById(product);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("产品编码已存在");
        }
        return toProductVO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        BomProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }
        List<Long> nodeIds = nodeMapper.selectList(new LambdaQueryWrapper<BomNode>()
                        .eq(BomNode::getProductId, id)).stream().map(BomNode::getId).collect(Collectors.toList());
        if (!nodeIds.isEmpty()) {
            nodeMaterialMapper.delete(new LambdaQueryWrapper<BomNodeMaterial>()
                    .in(BomNodeMaterial::getNodeId, nodeIds));
            nodeMapper.delete(new LambdaQueryWrapper<BomNode>().in(BomNode::getId, nodeIds));
        }
        productMapper.deleteById(id);
    }

    public BomProductVO changeProductStatus(Long id, Integer status) {
        BomProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }
        product.setStatus(status);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        return toProductVO(product);
    }

    public List<BomNodeVO> tree(Long productId) {
        List<BomNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<BomNode>()
                .eq(BomNode::getProductId, productId)
                .orderByAsc(BomNode::getLevel)
                .orderByAsc(BomNode::getSort)
                .orderByAsc(BomNode::getId));
        if (nodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> nodeIds = nodes.stream().map(BomNode::getId).collect(Collectors.toList());
        List<BomNodeMaterial> mats = nodeMaterialMapper.selectList(
                new LambdaQueryWrapper<BomNodeMaterial>().in(BomNodeMaterial::getNodeId, nodeIds));
        Map<Long, List<BomNodeMaterialVO>> matMap = mats.stream()
                .map(this::toMaterialVO)
                .collect(Collectors.groupingBy(BomNodeMaterialVO::getNodeId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, BomNodeVO> voMap = new LinkedHashMap<>();
        for (BomNode node : nodes) {
            BomNodeVO vo = BomNodeVO.from(node);
            vo.setMaterials(matMap.getOrDefault(node.getId(), new ArrayList<>()));
            vo.setChildren(new ArrayList<>());
            voMap.put(node.getId(), vo);
        }
        List<BomNodeVO> roots = new ArrayList<>();
        for (BomNodeVO vo : voMap.values()) {
            if (vo.getParentId() != null && voMap.containsKey(vo.getParentId())) {
                voMap.get(vo.getParentId()).getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }

    @Transactional
    public BomNodeVO saveNode(BomNodeRequest request) {
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("请选择所属产品");
        }
        BomProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("所属产品不存在");
        }
        if (!StringUtils.hasText(request.getNodeName())) {
            throw new IllegalArgumentException("请输入节点名称");
        }
        Integer level = 1;
        if (request.getParentId() != null) {
            BomNode parent = nodeMapper.selectById(request.getParentId());
            if (parent == null || !parent.getProductId().equals(request.getProductId())) {
                throw new IllegalArgumentException("父节点不存在或不属于该产品");
            }
            level = (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
            if (level > 10) {
                throw new IllegalArgumentException("BOM层级过深，最多支持10层");
            }
        }
        if (request.getId() == null) {
            BomNode node = new BomNode();
            node.setProductId(request.getProductId());
            node.setParentId(request.getParentId());
            node.setNodeName(request.getNodeName());
            node.setLevel(level);
            node.setSort(request.getSort() == null ? 0 : request.getSort());
            node.setRemark(request.getRemark());
            node.setCreateTime(LocalDateTime.now());
            node.setUpdateTime(LocalDateTime.now());
            nodeMapper.insert(node);
            return BomNodeVO.from(node);
        } else {
            BomNode node = nodeMapper.selectById(request.getId());
            if (node == null) {
                throw new IllegalArgumentException("节点不存在");
            }
            if (request.getParentId() != null && isDescendant(request.getId(), request.getParentId())) {
                throw new IllegalArgumentException("不能将节点挂到自身的子节点下");
            }
            node.setParentId(request.getParentId());
            node.setNodeName(request.getNodeName());
            node.setLevel(level);
            if (request.getSort() != null) {
                node.setSort(request.getSort());
            }
            node.setRemark(request.getRemark());
            node.setUpdateTime(LocalDateTime.now());
            nodeMapper.updateById(node);
            return BomNodeVO.from(node);
        }
    }

    @Transactional
    public void deleteNode(Long id) {
        BomNode node = nodeMapper.selectById(id);
        if (node == null) {
            throw new IllegalArgumentException("节点不存在");
        }
        List<Long> targetIds = new ArrayList<>();
        collectDescendantIds(id, targetIds);
        targetIds.add(id);
        nodeMaterialMapper.delete(new LambdaQueryWrapper<BomNodeMaterial>()
                .in(BomNodeMaterial::getNodeId, targetIds));
        nodeMapper.delete(new LambdaQueryWrapper<BomNode>().in(BomNode::getId, targetIds));
    }

    @Transactional
    public BomNodeMaterialVO addNodeMaterial(BomNodeMaterialRequest request) {
        if (request.getNodeId() == null || request.getMaterialId() == null) {
            throw new IllegalArgumentException("请选择节点与物料");
        }
        if (!BomNodeMaterialVO.MAIN.equals(request.getMaterialType())
                && !BomNodeMaterialVO.BACKUP.equals(request.getMaterialType())) {
            throw new IllegalArgumentException("物料类型必须是主料或备料");
        }
        BomNode node = nodeMapper.selectById(request.getNodeId());
        if (node == null) {
            throw new IllegalArgumentException("节点不存在");
        }
        Material material = materialMapper.selectById(request.getMaterialId());
        if (material == null) {
            throw new IllegalArgumentException("物料不存在");
        }
        if (BomNodeMaterialVO.MAIN.equals(request.getMaterialType())) {
            // 一个节点名称对应一个主料：先移除已有主料
            nodeMaterialMapper.delete(new LambdaQueryWrapper<BomNodeMaterial>()
                    .eq(BomNodeMaterial::getNodeId, request.getNodeId())
                    .eq(BomNodeMaterial::getMaterialType, BomNodeMaterialVO.MAIN));
        } else {
            BomNodeMaterial existing = nodeMaterialMapper.selectOne(new LambdaQueryWrapper<BomNodeMaterial>()
                    .eq(BomNodeMaterial::getNodeId, request.getNodeId())
                    .eq(BomNodeMaterial::getMaterialId, request.getMaterialId())
                    .eq(BomNodeMaterial::getMaterialType, BomNodeMaterialVO.BACKUP));
            if (existing != null) {
                existing.setQuantity(request.getQuantity());
                existing.setUpdateTime(LocalDateTime.now());
                nodeMaterialMapper.updateById(existing);
                return toMaterialVO(existing);
            }
        }
        BomNodeMaterial entity = new BomNodeMaterial();
        entity.setNodeId(request.getNodeId());
        entity.setMaterialId(request.getMaterialId());
        entity.setMaterialType(request.getMaterialType());
        entity.setQuantity(request.getQuantity() == null ? BigDecimal.ONE : request.getQuantity());
        entity.setSort(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        nodeMaterialMapper.insert(entity);
        return toMaterialVO(entity);
    }

    @Transactional
    public void removeNodeMaterial(Long id) {
        if (nodeMaterialMapper.selectById(id) == null) {
            throw new IllegalArgumentException("物料关联不存在");
        }
        nodeMaterialMapper.deleteById(id);
    }

    private void validateProduct(BomProductRequest request) {
        if (!StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("请输入产品编码");
        }
        if (request.getCode().length() > 50) {
            throw new IllegalArgumentException("产品编码长度不能超过50个字符");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请输入产品名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("产品名称长度不能超过100个字符");
        }
    }

    private void applyProduct(BomProduct product, BomProductRequest request) {
        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setSpec(request.getSpec());
        product.setCategory(request.getCategory());
        product.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
    }

    private BomProductVO toProductVO(BomProduct product) {
        BomProductVO vo = BomProductVO.from(product);
        List<Long> nodeIds = nodeMapper.selectList(new LambdaQueryWrapper<BomNode>()
                        .eq(BomNode::getProductId, product.getId())).stream()
                .map(BomNode::getId).collect(Collectors.toList());
        int nodeCount = nodeIds.size();
        int materialCount = 0;
        if (!nodeIds.isEmpty()) {
            materialCount = Math.toIntExact(nodeMaterialMapper.selectCount(
                    new LambdaQueryWrapper<BomNodeMaterial>().in(BomNodeMaterial::getNodeId, nodeIds)));
        } else {
            materialCount = 0;
        }
        vo.setNodeCount(nodeCount);
        vo.setMaterialCount(materialCount);
        return vo;
    }

    private BomNodeMaterialVO toMaterialVO(BomNodeMaterial entity) {
        BomNodeMaterialVO vo = BomNodeMaterialVO.from(entity);
        Material material = materialMapper.selectById(entity.getMaterialId());
        if (material != null) {
            vo.setMaterialSku(material.getSku());
            vo.setMaterialName(material.getName());
            vo.setMaterialSpec(material.getSpec());
            vo.setMaterialUnit(material.getUnit());
        }
        return vo;
    }

    private void collectDescendantIds(Long parentId, List<Long> accumulator) {
        List<BomNode> children = nodeMapper.selectList(
                new LambdaQueryWrapper<BomNode>().eq(BomNode::getParentId, parentId));
        for (BomNode child : children) {
            accumulator.add(child.getId());
            collectDescendantIds(child.getId(), accumulator);
        }
    }

    private boolean isDescendant(Long ancestorId, Long nodeId) {
        BomNode current = nodeMapper.selectById(nodeId);
        while (current != null && current.getParentId() != null) {
            if (current.getParentId().equals(ancestorId)) {
                return true;
            }
            current = nodeMapper.selectById(current.getParentId());
        }
        return false;
    }

    private Map<String, Object> computeStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long productTotal = productMapper.selectCount(null);
        long nodeTotal = nodeMapper.selectCount(null);
        long materialTotal = nodeMaterialMapper.selectCount(null);
        stats.put("productTotal", productTotal);
        stats.put("nodeTotal", nodeTotal);
        stats.put("materialTotal", materialTotal);
        return stats;
    }
}
