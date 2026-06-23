package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.material.MaterialBatchDeleteRequest;
import com.qws.common.dto.material.MaterialDeleteRequest;
import com.qws.common.dto.material.MaterialImportRequest;
import com.qws.common.dto.material.MaterialImportResult;
import com.qws.common.dto.material.MaterialListResult;
import com.qws.common.dto.material.MaterialRequest;
import com.qws.common.dto.material.MaterialStatusRequest;
import com.qws.common.dto.material.MaterialVO;
import com.qws.common.entity.Material;
import com.qws.common.entity.MaterialType;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.MaterialTypeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final MaterialTypeMapper materialTypeMapper;

    public MaterialService(MaterialMapper materialMapper, MaterialTypeMapper materialTypeMapper) {
        this.materialMapper = materialMapper;
        this.materialTypeMapper = materialTypeMapper;
    }

    private Map<String, String> categoryLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        materialTypeMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MaterialType>()
                .orderByAsc(MaterialType::getLevel)
                .orderByAsc(MaterialType::getSort)
                .orderByAsc(MaterialType::getId))
                .forEach(type -> labels.put(type.getCode(), type.getName()));
        return labels;
    }

    public MaterialListResult list(String keyword, String category, Integer status, int page, int pageSize) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Material::getSku, keyword)
                    .or().like(Material::getName, keyword)
                    .or().like(Material::getSpec, keyword)
                    .or().like(Material::getBarcode, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Material::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Material::getStatus, status);
        }
        wrapper.orderByDesc(Material::getUpdateTime);
        Page<Material> result = materialMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<MaterialVO> vos = result.getRecords().stream()
                .map(material -> MaterialVO.from(material, categoryLabels().get(material.getCategory())))
                .toList();
        MaterialListResult payload = new MaterialListResult();
        payload.setRows(vos);
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        payload.setCategories(categoryLabels().entrySet().stream()
                .map(entry -> Map.of("value", entry.getKey(), "label", entry.getValue()))
                .collect(Collectors.toList()));
        return payload;
    }

    public MaterialListResult export(String keyword, String category, Integer status) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Material::getSku, keyword)
                    .or().like(Material::getName, keyword)
                    .or().like(Material::getSpec, keyword)
                    .or().like(Material::getBarcode, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Material::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Material::getStatus, status);
        }
        wrapper.orderByDesc(Material::getUpdateTime);
        List<MaterialVO> vos = materialMapper.selectList(wrapper).stream()
                .map(material -> MaterialVO.from(material, categoryLabels().get(material.getCategory())))
                .toList();
        MaterialListResult payload = new MaterialListResult();
        payload.setRows(vos);
        payload.setTotal(vos.size());
        payload.setStats(computeStats());
        return payload;
    }

    public MaterialVO detail(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new IllegalArgumentException("物料不存在");
        }
        return MaterialVO.from(material, categoryLabels().get(material.getCategory()));
    }

    public MaterialVO create(MaterialRequest request) {
        validate(request);
        if (!StringUtils.hasText(request.getSku())) {
            throw new IllegalArgumentException("SKU编码不能为空");
        }
        Material exists = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getSku, request.getSku()));
        if (exists != null) {
            throw new IllegalStateException("SKU编码已存在");
        }
        Material material = new Material();
        applyRequest(material, request);
        if (material.getStatus() == null) {
            material.setStatus(1);
        }
        material.setCreateTime(LocalDateTime.now());
        material.setUpdateTime(LocalDateTime.now());
        try {
            materialMapper.insert(material);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("SKU编码已存在");
        }
        return MaterialVO.from(material, categoryLabels().get(material.getCategory()));
    }

    public MaterialVO update(MaterialRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的物料");
        }
        validate(request);
        Material material = materialMapper.selectById(request.getId());
        if (material == null) {
            throw new IllegalArgumentException("物料不存在");
        }
        if (!material.getSku().equals(request.getSku())) {
            Material conflict = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                    .eq(Material::getSku, request.getSku())
                    .ne(Material::getId, request.getId()));
            if (conflict != null) {
                throw new IllegalStateException("SKU编码已存在");
            }
        }
        applyRequest(material, request);
        material.setUpdateTime(LocalDateTime.now());
        try {
            materialMapper.updateById(material);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("SKU编码已存在");
        }
        return MaterialVO.from(material, categoryLabels().get(material.getCategory()));
    }

    @Transactional
    public void delete(MaterialDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的物料");
        }
        Material material = materialMapper.selectById(request.getId());
        if (material == null) {
            throw new IllegalArgumentException("物料不存在");
        }
        materialMapper.deleteById(request.getId());
    }

    @Transactional
    public int batchDelete(MaterialBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }
        return materialMapper.deleteByIds(request.getIds());
    }

    public MaterialVO changeStatus(MaterialStatusRequest request) {
        if (request.getId() == null || request.getStatus() == null) {
            throw new IllegalArgumentException("参数不完整");
        }
        Material material = materialMapper.selectById(request.getId());
        if (material == null) {
            throw new IllegalArgumentException("物料不存在");
        }
        material.setStatus(request.getStatus());
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
        return MaterialVO.from(material, categoryLabels().get(material.getCategory()));
    }

    public MaterialImportResult importBatch(MaterialImportRequest request) {
        MaterialImportResult result = new MaterialImportResult();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            result.setSuccessCount(0);
            result.setFailedCount(0);
            result.setItems(new ArrayList<>());
            result.setFailedSkus(new ArrayList<>());
            return result;
        }
        List<MaterialVO> created = new ArrayList<>();
        List<String> failedSkus = new ArrayList<>();
        int failed = 0;
        for (MaterialRequest item : request.getItems()) {
            try {
                if (!StringUtils.hasText(item.getSku())) {
                    throw new IllegalArgumentException("SKU编码为空");
                }
                validate(item);
                Material exists = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                        .eq(Material::getSku, item.getSku()));
                if (exists != null) {
                    throw new IllegalStateException("SKU编码已存在");
                }
                Material material = new Material();
                applyRequest(material, item);
                if (material.getStatus() == null) {
                    material.setStatus(1);
                }
                material.setCreateTime(LocalDateTime.now());
                material.setUpdateTime(LocalDateTime.now());
                materialMapper.insert(material);
                created.add(MaterialVO.from(material, categoryLabels().get(material.getCategory())));
            } catch (Exception ex) {
                failed++;
                failedSkus.add(item.getSku() + "(" + ex.getMessage() + ")");
            }
        }
        result.setItems(created);
        result.setSuccessCount(created.size());
        result.setFailedCount(failed);
        result.setFailedSkus(failedSkus);
        return result;
    }

    private void validate(MaterialRequest request) {
        if (!StringUtils.hasText(request.getSku())) {
            throw new IllegalArgumentException("请填写SKU编码");
        }
        if (request.getSku().length() > 50) {
            throw new IllegalArgumentException("SKU编码长度不能超过50个字符");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写物料名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("物料名称长度不能超过100个字符");
        }
        if (!StringUtils.hasText(request.getCategory())) {
            throw new IllegalArgumentException("请选择物料类型");
        }
        if (!categoryLabels().containsKey(request.getCategory())) {
            throw new IllegalArgumentException("物料类型不合法");
        }
        if (!StringUtils.hasText(request.getUnit())) {
            throw new IllegalArgumentException("请填写计量单位");
        }
        if (request.getUnitPrice() != null && request.getUnitPrice().signum() < 0) {
            throw new IllegalArgumentException("单价不能小于0");
        }
        if (request.getSafeStock() != null && request.getSafeStock() < 0) {
            throw new IllegalArgumentException("安全库存不能小于0");
        }
    }

    private void applyRequest(Material material, MaterialRequest request) {
        material.setSku(request.getSku());
        material.setName(request.getName());
        material.setSpec(request.getSpec());
        material.setCategory(request.getCategory());
        material.setBrand(request.getBrand());
        material.setUnit(request.getUnit());
        material.setUnitPrice(request.getUnitPrice() == null ? BigDecimal.ZERO : request.getUnitPrice());
        material.setSafeStock(request.getSafeStock() == null ? 0 : request.getSafeStock());
        material.setSupplier(request.getSupplier());
        material.setBarcode(request.getBarcode());
        material.setShelfLifeDays(request.getShelfLifeDays());
        material.setWeight(request.getWeight());
        material.setVolume(request.getVolume());
        material.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            material.setStatus(request.getStatus());
        }
    }

    private Map<String, Object> computeStats() {
        List<Material> all = materialMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("activeCount", all.stream().filter(m -> m.getStatus() != null && m.getStatus() == 1).count());
        stats.put("inactiveCount", all.stream().filter(m -> m.getStatus() != null && m.getStatus() == 0).count());
        Map<String, Long> byCategory = all.stream()
                .filter(m -> m.getCategory() != null)
                .collect(Collectors.groupingBy(Material::getCategory, LinkedHashMap::new, Collectors.counting()));
        Map<String, String> byCategoryLabel = new LinkedHashMap<>();
        byCategory.forEach((key, value) -> byCategoryLabel.put(categoryLabels().getOrDefault(key, key), String.valueOf(value)));
        stats.put("byCategory", byCategoryLabel);
        Map<String, Long> byBrand = all.stream()
                .filter(m -> m.getBrand() != null && !m.getBrand().isEmpty())
                .collect(Collectors.groupingBy(Material::getBrand, LinkedHashMap::new, Collectors.counting()));
        stats.put("brandCount", byBrand.size());
        return stats;
    }
}

