package com.qws.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.dto.supplier.SupplierRequest;
import com.qws.common.dto.supplier.SupplierStatusRequest;
import com.qws.common.dto.supplier.SupplierVO;
import com.qws.common.entity.Material;
import com.qws.common.entity.Supplier;
import com.qws.common.mapper.MaterialMapper;
import com.qws.common.mapper.SupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final MaterialMapper materialMapper;

    public SupplierService(SupplierMapper supplierMapper, MaterialMapper materialMapper) {
        this.supplierMapper = supplierMapper;
        this.materialMapper = materialMapper;
    }

    public List<SupplierVO> list(String keyword, String category, Integer status) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Supplier::getCode, keyword)
                    .or().like(Supplier::getName, keyword)
                    .or().like(Supplier::getShortName, keyword)
                    .or().like(Supplier::getContact, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Supplier::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Supplier::getStatus, status);
        }
        wrapper.orderByAsc(Supplier::getSort).orderByAsc(Supplier::getId);
        Map<String, Integer> counts = countMaterialsBySupplier();
        return supplierMapper.selectList(wrapper).stream().map(supplier -> {
            SupplierVO vo = SupplierVO.from(supplier);
            vo.setMaterialCount(counts.getOrDefault(supplier.getName(), 0));
            return vo;
        }).toList();
    }

    public SupplierVO detail(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        SupplierVO vo = SupplierVO.from(supplier);
        vo.setMaterialCount(countMaterialsBySupplier().getOrDefault(supplier.getName(), 0));
        return vo;
    }

    @Transactional
    public SupplierVO create(SupplierRequest request) {
        validate(request, false);
        if (supplierMapper.selectOne(new LambdaQueryWrapper<Supplier>().eq(Supplier::getCode, request.getCode())) != null) {
            throw new IllegalStateException("供应商编码已存在");
        }
        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());
        supplierMapper.insert(supplier);
        return detail(supplier.getId());
    }

    @Transactional
    public SupplierVO update(SupplierRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("缺少供应商ID");
        }
        Supplier supplier = supplierMapper.selectById(request.getId());
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(supplier.getCode())) {
            if (supplierMapper.selectOne(new LambdaQueryWrapper<Supplier>()
                    .eq(Supplier::getCode, request.getCode())
                    .ne(Supplier::getId, request.getId())) != null) {
                throw new IllegalStateException("供应商编码已存在");
            }
        }
        applyRequest(supplier, request);
        supplier.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(supplier);
        return detail(supplier.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少供应商ID");
        }
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        Long materials = materialMapper.selectCount(new LambdaQueryWrapper<Material>().eq(Material::getSupplier, supplier.getName()));
        if (materials != null && materials > 0) {
            throw new IllegalStateException("该供应商下已绑定物料，无法删除");
        }
        supplierMapper.deleteById(id);
    }

    @Transactional
    public SupplierVO changeStatus(SupplierStatusRequest request) {
        if (request.getId() == null || request.getStatus() == null) {
            throw new IllegalArgumentException("参数不完整");
        }
        Supplier supplier = supplierMapper.selectById(request.getId());
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        supplier.setStatus(request.getStatus());
        supplier.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(supplier);
        return detail(supplier.getId());
    }

    public Map<String, Object> stats() {
        List<Supplier> suppliers = supplierMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", suppliers.size());
        stats.put("activeCount", suppliers.stream().filter(s -> s.getStatus() != null && s.getStatus() == 1).count());
        stats.put("inactiveCount", suppliers.stream().filter(s -> s.getStatus() != null && s.getStatus() == 0).count());
        stats.put("materialTotal", countMaterialsBySupplier().values().stream().mapToInt(Integer::intValue).sum());
        return stats;
    }

    private void validate(SupplierRequest request, boolean isUpdate) {
        if (!isUpdate && !StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("请填写供应商编码");
        }
        if (StringUtils.hasText(request.getCode()) && request.getCode().length() > 50) {
            throw new IllegalArgumentException("供应商编码长度不能超过50");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写供应商名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("供应商名称长度不能超过100");
        }
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        if (StringUtils.hasText(request.getCode())) supplier.setCode(request.getCode());
        supplier.setName(request.getName());
        supplier.setShortName(request.getShortName());
        supplier.setCategory(request.getCategory());
        supplier.setContact(request.getContact());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setTaxNo(request.getTaxNo());
        supplier.setBankAccount(request.getBankAccount());
        supplier.setSettleType(request.getSettleType());
        supplier.setDescription(request.getDescription());
        supplier.setSort(request.getSort() == null ? 0 : request.getSort());
        supplier.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private Map<String, Integer> countMaterialsBySupplier() {
        Map<String, Integer> counts = new HashMap<>();
        materialMapper.selectList(null).forEach(material -> {
            if (StringUtils.hasText(material.getSupplier())) {
                counts.merge(material.getSupplier(), 1, Integer::sum);
            }
        });
        return counts;
    }
}
