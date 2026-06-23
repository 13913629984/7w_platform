package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.brand.BrandRequest;
import com.qws.common.dto.brand.BrandStatusRequest;
import com.qws.common.dto.brand.BrandVO;
import com.qws.common.entity.Brand;
import com.qws.common.entity.Material;
import com.qws.common.mapper.BrandMapper;
import com.qws.common.mapper.MaterialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrandService {

    private final BrandMapper brandMapper;
    private final MaterialMapper materialMapper;

    public BrandService(BrandMapper brandMapper, MaterialMapper materialMapper) {
        this.brandMapper = brandMapper;
        this.materialMapper = materialMapper;
    }

    public List<BrandVO> list(String keyword, Integer status) {
        LambdaQueryWrapper<Brand> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Brand::getCode, keyword)
                    .or().like(Brand::getName, keyword)
                    .or().like(Brand::getEnglishName, keyword)
                    .or().like(Brand::getSupplier, keyword));
        }
        if (status != null) {
            wrapper.eq(Brand::getStatus, status);
        }
        wrapper.orderByAsc(Brand::getSort).orderByAsc(Brand::getId);
        Map<String, Integer> counts = countMaterialsByBrand();
        return brandMapper.selectList(wrapper).stream().map(brand -> {
            BrandVO vo = BrandVO.from(brand);
            vo.setMaterialCount(counts.getOrDefault(brand.getName(), 0));
            return vo;
        }).toList();
    }

    public BrandVO detail(Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        BrandVO vo = BrandVO.from(brand);
        vo.setMaterialCount(countMaterialsByBrand().getOrDefault(brand.getName(), 0));
        return vo;
    }

    @Transactional
    public BrandVO create(BrandRequest request) {
        validate(request, false);
        if (brandMapper.selectOne(new LambdaQueryWrapper<Brand>().eq(Brand::getCode, request.getCode())) != null) {
            throw new IllegalStateException("品牌编码已存在");
        }
        Brand brand = new Brand();
        applyRequest(brand, request);
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brandMapper.insert(brand);
        return detail(brand.getId());
    }

    @Transactional
    public BrandVO update(BrandRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("缺少品牌ID");
        }
        Brand brand = brandMapper.selectById(request.getId());
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(brand.getCode())) {
            if (brandMapper.selectOne(new LambdaQueryWrapper<Brand>()
                    .eq(Brand::getCode, request.getCode())
                    .ne(Brand::getId, request.getId())) != null) {
                throw new IllegalStateException("品牌编码已存在");
            }
        }
        applyRequest(brand, request);
        brand.setUpdateTime(LocalDateTime.now());
        brandMapper.updateById(brand);
        return detail(brand.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少品牌ID");
        }
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        Long materials = materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getBrand, brand.getName()));
        if (materials != null && materials > 0) {
            throw new IllegalStateException("该品牌下已绑定物料，无法删除");
        }
        brandMapper.deleteById(id);
    }

    @Transactional
    public BrandVO changeStatus(BrandStatusRequest request) {
        if (request.getId() == null || request.getStatus() == null) {
            throw new IllegalArgumentException("参数不完整");
        }
        Brand brand = brandMapper.selectById(request.getId());
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        brand.setStatus(request.getStatus());
        brand.setUpdateTime(LocalDateTime.now());
        brandMapper.updateById(brand);
        return detail(brand.getId());
    }

    public Map<String, Object> stats() {
        List<Brand> brands = brandMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", brands.size());
        stats.put("activeCount", brands.stream().filter(b -> b.getStatus() != null && b.getStatus() == 1).count());
        stats.put("inactiveCount", brands.stream().filter(b -> b.getStatus() != null && b.getStatus() == 0).count());
        stats.put("materialTotal", countMaterialsByBrand().values().stream().mapToInt(Integer::intValue).sum());
        return stats;
    }

    private void validate(BrandRequest request, boolean isUpdate) {
        if (!isUpdate && !StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("请填写品牌编码");
        }
        if (StringUtils.hasText(request.getCode()) && request.getCode().length() > 50) {
            throw new IllegalArgumentException("品牌编码长度不能超过50");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写品牌名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("品牌名称长度不能超过100");
        }
    }

    private void applyRequest(Brand brand, BrandRequest request) {
        if (StringUtils.hasText(request.getCode())) brand.setCode(request.getCode());
        brand.setName(request.getName());
        brand.setEnglishName(request.getEnglishName());
        brand.setLogo(request.getLogo());
        brand.setCountry(request.getCountry());
        brand.setSupplier(request.getSupplier());
        brand.setContact(request.getContact());
        brand.setPhone(request.getPhone());
        brand.setDescription(request.getDescription());
        brand.setSort(request.getSort() == null ? 0 : request.getSort());
        brand.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private Map<String, Integer> countMaterialsByBrand() {
        Map<String, Integer> counts = new HashMap<>();
        materialMapper.selectList(null).forEach(material -> {
            if (StringUtils.hasText(material.getBrand())) {
                counts.merge(material.getBrand(), 1, Integer::sum);
            }
        });
        return counts;
    }
}