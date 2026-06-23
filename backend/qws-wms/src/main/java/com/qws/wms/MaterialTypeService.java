package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.materialtype.MaterialTypeRequest;
import com.qws.common.dto.materialtype.MaterialTypeStatusRequest;
import com.qws.common.dto.materialtype.MaterialTypeVO;
import com.qws.common.entity.Material;
import com.qws.common.entity.MaterialType;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.MaterialTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaterialTypeService {

    private final MaterialTypeMapper typeMapper;
    private final MaterialMapper materialMapper;

    public MaterialTypeService(MaterialTypeMapper typeMapper, MaterialMapper materialMapper) {
        this.typeMapper = typeMapper;
        this.materialMapper = materialMapper;
    }

    public List<MaterialTypeVO> tree(String keyword) {
        List<MaterialType> all = typeMapper.selectList(new LambdaQueryWrapper<MaterialType>()
                .orderByAsc(MaterialType::getLevel)
                .orderByAsc(MaterialType::getSort)
                .orderByAsc(MaterialType::getId));
        Map<String, Integer> counts = countByCategory();
        List<MaterialTypeVO> vos = all.stream().map(MaterialTypeVO::from).collect(Collectors.toList());
        Map<String, MaterialTypeVO> indexByCode = new HashMap<>();
        for (MaterialTypeVO vo : vos) {
            vo.setMaterialCount(counts.getOrDefault(vo.getCode(), 0));
            indexByCode.put(vo.getCode(), vo);
        }
        List<MaterialTypeVO> roots = new ArrayList<>();
        for (MaterialTypeVO vo : vos) {
            if (StringUtils.hasText(vo.getParentCode())) {
                MaterialTypeVO parent = indexByCode.get(vo.getParentCode());
                if (parent != null) {
                    vo.setParentName(parent.getName());
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            } else {
                roots.add(vo);
            }
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            return filter(roots, kw);
        }
        return roots;
    }

    public List<MaterialTypeVO> flatList() {
        return typeMapper.selectList(new LambdaQueryWrapper<MaterialType>()
                        .orderByAsc(MaterialType::getLevel)
                        .orderByAsc(MaterialType::getSort)
                        .orderByAsc(MaterialType::getId))
                .stream().map(MaterialTypeVO::from).collect(Collectors.toList());
    }

    public MaterialTypeVO detail(Long id) {
        MaterialType type = typeMapper.selectById(id);
        if (type == null) {
            throw new IllegalArgumentException("类型不存在");
        }
        MaterialTypeVO vo = MaterialTypeVO.from(type);
        if (StringUtils.hasText(type.getParentCode())) {
            MaterialType parent = typeMapper.selectOne(new LambdaQueryWrapper<MaterialType>()
                    .eq(MaterialType::getCode, type.getParentCode()));
            if (parent != null) {
                vo.setParentName(parent.getName());
            }
        }
        vo.setMaterialCount(countByCategory().getOrDefault(type.getCode(), 0));
        return vo;
    }

    @Transactional
    public MaterialTypeVO create(MaterialTypeRequest request) {
        validate(request, false);
        if (typeMapper.selectOne(new LambdaQueryWrapper<MaterialType>().eq(MaterialType::getCode, request.getCode())) != null) {
            throw new IllegalStateException("类型编码已存在");
        }
        MaterialType type = new MaterialType();
        applyRequest(type, request);
        type.setCreateTime(LocalDateTime.now());
        type.setUpdateTime(LocalDateTime.now());
        typeMapper.insert(type);
        return MaterialTypeVO.from(type);
    }

    @Transactional
    public MaterialTypeVO update(MaterialTypeRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("缺少类型ID");
        }
        MaterialType existing = typeMapper.selectById(request.getId());
        if (existing == null) {
            throw new IllegalArgumentException("类型不存在");
        }
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(existing.getCode())) {
            if (typeMapper.selectOne(new LambdaQueryWrapper<MaterialType>()
                    .eq(MaterialType::getCode, request.getCode())
                    .ne(MaterialType::getId, existing.getId())) != null) {
                throw new IllegalStateException("类型编码已存在");
            }
        }
        applyRequest(existing, request);
        existing.setUpdateTime(LocalDateTime.now());
        typeMapper.updateById(existing);
        return MaterialTypeVO.from(existing);
    }

    @Transactional
    public void delete(Long id) {
        MaterialType existing = typeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("类型不存在");
        }
        Long children = typeMapper.selectCount(new LambdaQueryWrapper<MaterialType>()
                .eq(MaterialType::getParentCode, existing.getCode()));
        if (children != null && children > 0) {
            throw new IllegalStateException("该类型下存在子类型，无法删除");
        }
        Long materials = materialMapper.selectCount(new LambdaQueryWrapper<Material>()
                .eq(Material::getCategory, existing.getCode()));
        if (materials != null && materials > 0) {
            throw new IllegalStateException("该类型下已绑定物料，无法删除");
        }
        typeMapper.deleteById(id);
    }

    @Transactional
    public MaterialTypeVO changeStatus(MaterialTypeStatusRequest request) {
        if (request.getId() == null || request.getStatus() == null) {
            throw new IllegalArgumentException("参数不完整");
        }
        MaterialType existing = typeMapper.selectById(request.getId());
        if (existing == null) {
            throw new IllegalArgumentException("类型不存在");
        }
        existing.setStatus(request.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        typeMapper.updateById(existing);
        return MaterialTypeVO.from(existing);
    }

    private void validate(MaterialTypeRequest request, boolean isUpdate) {
        if (!isUpdate && !StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("请填写类型编码");
        }
        if (request.getCode() != null && request.getCode().length() > 50) {
            throw new IllegalArgumentException("类型编码长度不能超过50");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写类型名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("类型名称长度不能超过100");
        }
        if (StringUtils.hasText(request.getParentCode())) {
            MaterialType parent = typeMapper.selectOne(new LambdaQueryWrapper<MaterialType>()
                    .eq(MaterialType::getCode, request.getParentCode()));
            if (parent == null) {
                throw new IllegalArgumentException("上级类型不存在");
            }
            if (parent.getLevel() != null && parent.getLevel() >= 3) {
                throw new IllegalArgumentException("最多支持三级类型");
            }
            if (request.getId() != null && parent.getId().equals(request.getId())) {
                throw new IllegalArgumentException("上级类型不能为自身");
            }
        }
    }

    private void applyRequest(MaterialType target, MaterialTypeRequest request) {
        if (StringUtils.hasText(request.getCode())) {
            target.setCode(request.getCode());
        }
        target.setName(request.getName());
        target.setDescription(request.getDescription());
        target.setParentCode(StringUtils.hasText(request.getParentCode()) ? request.getParentCode() : null);
        if (StringUtils.hasText(request.getParentCode())) {
            MaterialType parent = typeMapper.selectOne(new LambdaQueryWrapper<MaterialType>()
                    .eq(MaterialType::getCode, request.getParentCode()));
            target.setLevel(parent != null && parent.getLevel() != null ? parent.getLevel() + 1 : 2);
        } else {
            target.setLevel(1);
        }
        target.setSort(request.getSort() != null ? request.getSort() : 0);
        target.setStatus(request.getStatus() != null ? request.getStatus() : 1);
    }

    private Map<String, Integer> countByCategory() {
        List<Material> all = materialMapper.selectList(null);
        Map<String, Integer> counts = new HashMap<>();
        for (Material material : all) {
            if (material.getCategory() == null) continue;
            counts.merge(material.getCategory(), 1, Integer::sum);
        }
        return counts;
    }

    private List<MaterialTypeVO> filter(List<MaterialTypeVO> nodes, String keyword) {
        List<MaterialTypeVO> result = new ArrayList<>();
        for (MaterialTypeVO node : nodes) {
            List<MaterialTypeVO> filteredChildren = filter(node.getChildren(), keyword);
            boolean selfMatch = (node.getName() != null && node.getName().toLowerCase().contains(keyword))
                    || (node.getCode() != null && node.getCode().toLowerCase().contains(keyword));
            if (selfMatch || !filteredChildren.isEmpty()) {
                node.setChildren(filteredChildren);
                result.add(node);
            }
        }
        result.sort(Comparator.comparing(v -> v.getSort() == null ? 0 : v.getSort()));
        return result;
    }
}