package com.qws.maint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.maint.MaintListResult;
import com.qws.common.dto.maint.SparePartRequest;
import com.qws.common.dto.maint.SparePartVO;
import com.qws.common.entity.MaintSparePart;
import com.qws.common.mapper.MaintSparePartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维保-备件服务：备件台账维护、库存安全预警统计。
 */
@Service
public class SparePartService {

    private final MaintSparePartMapper sparePartMapper;

    public SparePartService(MaintSparePartMapper sparePartMapper) {
        this.sparePartMapper = sparePartMapper;
    }

    public MaintListResult<SparePartVO> list(String keyword, String category, String warehouseCode,
                                             Boolean lowStockOnly, int page, int pageSize) {
        LambdaQueryWrapper<MaintSparePart> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MaintSparePart::getCode, keyword)
                    .or().like(MaintSparePart::getName, keyword)
                    .or().like(MaintSparePart::getSpec, keyword)
                    .or().like(MaintSparePart::getSupplier, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(MaintSparePart::getCategory, category);
        }
        if (StringUtils.hasText(warehouseCode)) {
            wrapper.eq(MaintSparePart::getWarehouseCode, warehouseCode);
        }
        // 低库存过滤必须下推到 SQL，否则分页后再过滤会导致 total 与实际行数不一致、甚至整页为空
        if (Boolean.TRUE.equals(lowStockOnly)) {
            wrapper.apply("stock_qty < safety_qty");
        }
        wrapper.orderByAsc(MaintSparePart::getCode);
        Page<MaintSparePart> result = sparePartMapper.selectPage(
                new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);

        List<SparePartVO> rows = result.getRecords().stream().map(SparePartVO::from).toList();

        MaintListResult<SparePartVO> payload = new MaintListResult<>();
        payload.setRows(rows);
        payload.setTotal(result.getTotal());
        payload.setPage(normalizePage(page));
        payload.setPageSize(normalizePageSize(pageSize));
        payload.setStats(stats());
        return payload;
    }

    public SparePartVO detail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少备件ID");
        }
        MaintSparePart sparePart = sparePartMapper.selectById(id);
        if (sparePart == null) {
            throw new IllegalArgumentException("备件不存在");
        }
        return SparePartVO.from(sparePart);
    }

    @Transactional
    public SparePartVO create(SparePartRequest request) {
        validate(request, false);
        if (StringUtils.hasText(request.getCode()) && sparePartMapper.selectCount(
                new LambdaQueryWrapper<MaintSparePart>().eq(MaintSparePart::getCode, request.getCode())) > 0) {
            throw new IllegalArgumentException("备件编码已存在");
        }
        MaintSparePart sparePart = new MaintSparePart();
        applyRequest(sparePart, request);
        if (!StringUtils.hasText(sparePart.getCode())) {
            sparePart.setCode("SP" + String.format("%04d", sparePartMapper.selectCount(null) + 1));
        }
        sparePart.setCreateTime(LocalDateTime.now());
        sparePart.setUpdateTime(LocalDateTime.now());
        sparePartMapper.insert(sparePart);
        return SparePartVO.from(sparePart);
    }

    @Transactional
    public SparePartVO update(SparePartRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的备件");
        }
        MaintSparePart sparePart = sparePartMapper.selectById(request.getId());
        if (sparePart == null) {
            throw new IllegalArgumentException("备件不存在");
        }
        validate(request, true);
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(sparePart.getCode())) {
            if (sparePartMapper.selectCount(new LambdaQueryWrapper<MaintSparePart>()
                    .eq(MaintSparePart::getCode, request.getCode())
                    .ne(MaintSparePart::getId, request.getId())) > 0) {
                throw new IllegalArgumentException("备件编码已存在");
            }
        }
        applyRequest(sparePart, request);
        sparePart.setUpdateTime(LocalDateTime.now());
        sparePartMapper.updateById(sparePart);
        return SparePartVO.from(sparePart);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少备件ID");
        }
        MaintSparePart sparePart = sparePartMapper.selectById(id);
        if (sparePart == null) {
            throw new IllegalArgumentException("备件不存在");
        }
        sparePartMapper.deleteById(id);
    }

    /** 备件统计卡：总数、分类数、低库存数、库存总值。 */
    public Map<String, Object> stats() {
        List<MaintSparePart> spareParts = sparePartMapper.selectList(null);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", spareParts.size());
        stats.put("categoryCount", spareParts.stream()
                .map(MaintSparePart::getCategory).filter(StringUtils::hasText).distinct().count());
        stats.put("lowStockCount", spareParts.stream().filter(this::isLowStock).count());
        stats.put("stockValue", spareParts.stream().map(this::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.put("disabledCount", spareParts.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 0).count());
        return stats;
    }

    private boolean isLowStock(MaintSparePart sparePart) {
        return sparePart.getStockQty() != null && sparePart.getSafetyQty() != null
                && sparePart.getStockQty().compareTo(sparePart.getSafetyQty()) < 0;
    }

    private BigDecimal stockValue(MaintSparePart sparePart) {
        if (sparePart.getStockQty() == null || sparePart.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return sparePart.getStockQty().multiply(sparePart.getPrice());
    }

    private void validate(SparePartRequest request, boolean isUpdate) {
        if (request == null) {
            throw new IllegalArgumentException("请求数据不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写备件名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("备件名称长度不能超过100");
        }
        if (request.getStockQty() != null && request.getStockQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("库存数量不能为负数");
        }
    }

    private void applyRequest(MaintSparePart sparePart, SparePartRequest request) {
        if (StringUtils.hasText(request.getCode())) sparePart.setCode(request.getCode());
        sparePart.setName(request.getName());
        sparePart.setSpec(request.getSpec());
        sparePart.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit() : "个");
        sparePart.setCategory(request.getCategory());
        sparePart.setWarehouseCode(request.getWarehouseCode());
        sparePart.setStockQty(request.getStockQty() == null ? BigDecimal.ZERO : request.getStockQty());
        sparePart.setSafetyQty(request.getSafetyQty() == null ? BigDecimal.ZERO : request.getSafetyQty());
        sparePart.setPrice(request.getPrice());
        sparePart.setSupplier(request.getSupplier());
        sparePart.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        sparePart.setRemark(request.getRemark());
    }

    private int normalizePage(int page) {
        return page <= 0 ? 1 : page;
    }

    private int normalizePageSize(int pageSize) {
        return pageSize <= 0 ? 10 : pageSize;
    }
}
