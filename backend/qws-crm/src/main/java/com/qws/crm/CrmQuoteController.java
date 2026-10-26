package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.quote.QuoteDeleteRequest;
import com.qws.common.dto.quote.QuoteListResult;
import com.qws.common.dto.quote.QuoteRequest;
import com.qws.common.dto.quote.QuoteVO;
import com.qws.common.entity.CrmQuote;
import com.qws.common.mapper.CrmQuoteMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequirePermission("CRM")
@RestController
@RequestMapping("/crm/quote")
public class CrmQuoteController {

    private final CrmQuoteMapper quoteMapper;

    public CrmQuoteController(CrmQuoteMapper quoteMapper) {
        this.quoteMapper = quoteMapper;
    }

    @GetMapping("/list")
    public ApiResult<QuoteListResult> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String customerName,
                                           @RequestParam(required = false) String brand,
                                           @RequestParam(required = false) String model,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<CrmQuote> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmQuote::getCustomerName, keyword)
                    .or().like(CrmQuote::getBrand, keyword)
                    .or().like(CrmQuote::getModel, keyword)
                    .or().like(CrmQuote::getVisitSubject, keyword));
        }
        if (StringUtils.hasText(customerName)) wrapper.like(CrmQuote::getCustomerName, customerName.trim());
        if (StringUtils.hasText(brand)) wrapper.like(CrmQuote::getBrand, brand.trim());
        if (StringUtils.hasText(model)) wrapper.like(CrmQuote::getModel, model.trim());
        wrapper.orderByDesc(CrmQuote::getCreateTime).orderByDesc(CrmQuote::getId);

        Page<CrmQuote> result = quoteMapper.selectPage(new Page<>(page, pageSize), wrapper);
        QuoteListResult payload = new QuoteListResult();
        payload.setRows(result.getRecords().stream().map(QuoteVO::from).toList());
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        return ApiResult.ok(payload);
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(computeStats());
    }

    @GetMapping("/brands")
    public ApiResult<List<String>> brands() {
        List<String> brands = quoteMapper.selectList(null).stream()
                .map(CrmQuote::getBrand)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
        return ApiResult.ok(brands);
    }

    @GetMapping("/{id}")
    public ApiResult<QuoteVO> detail(@PathVariable Long id) {
        CrmQuote quote = quoteMapper.selectById(id);
        if (quote == null) return ApiResult.fail("报价不存在");
        return ApiResult.ok(QuoteVO.from(quote));
    }

    @PostMapping("/create")
    @Audit(module = "CRM", action = ActionType.CREATE, target = "报价")
    public ApiResult<QuoteVO> create(@RequestBody QuoteRequest request) {
        try {
            validate(request);
            CrmQuote quote = new CrmQuote();
            apply(quote, request);
            quote.setCreateTime(LocalDateTime.now());
            quoteMapper.insert(quote);
            return ApiResult.ok(QuoteVO.from(quote));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    @Audit(module = "CRM", action = ActionType.UPDATE, target = "报价")
    public ApiResult<QuoteVO> update(@RequestBody QuoteRequest request) {
        try {
            if (request.getId() == null) throw new IllegalArgumentException("请选择要修改的报价");
            validate(request);
            CrmQuote quote = quoteMapper.selectById(request.getId());
            if (quote == null) throw new IllegalArgumentException("报价不存在");
            apply(quote, request);
            quoteMapper.updateById(quote);
            return ApiResult.ok(QuoteVO.from(quote));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    @Audit(module = "CRM", action = ActionType.DELETE, target = "报价")
    public ApiResult<Void> delete(@RequestBody QuoteDeleteRequest request) {
        try {
            if (request.getId() == null && (request.getIds() == null || request.getIds().isEmpty())) {
                throw new IllegalArgumentException("请选择要删除的报价");
            }
            if (request.getId() != null) {
                quoteMapper.deleteById(request.getId());
            } else {
                quoteMapper.deleteByIds(request.getIds());
            }
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    private void validate(QuoteRequest request) {
        if (!StringUtils.hasText(request.getCustomerName())) {
            throw new IllegalArgumentException("请填写客户名称");
        }
    }

    private void apply(CrmQuote quote, QuoteRequest request) {
        quote.setCustomerName(request.getCustomerName().trim());
        quote.setBrand(request.getBrand());
        quote.setModel(request.getModel());
        quote.setPrice(request.getPrice());
        quote.setVisitSubject(request.getVisitSubject());
    }

    private Map<String, Object> computeStats() {
        List<CrmQuote> all = quoteMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);

        BigDecimal totalAmount = all.stream()
                .map(CrmQuote::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPrice = all.isEmpty() ? BigDecimal.ZERO
                : totalAmount.divide(BigDecimal.valueOf(all.size()), 2, java.math.RoundingMode.HALF_UP);

        stats.put("total", all.size());
        stats.put("customerCount", all.stream().map(CrmQuote::getCustomerName).filter(StringUtils::hasText).distinct().count());
        stats.put("brandCount", all.stream().map(CrmQuote::getBrand).filter(StringUtils::hasText).distinct().count());
        stats.put("totalAmount", totalAmount);
        stats.put("avgPrice", avgPrice);
        stats.put("monthAdded", all.stream()
                .filter(q -> q.getCreateTime() != null && !q.getCreateTime().toLocalDate().isBefore(firstDayOfMonth))
                .count());
        stats.put("brandDistribution", brandDistribution(all));
        return stats;
    }

    private List<Map<String, Object>> brandDistribution(List<CrmQuote> all) {
        Map<String, Integer> counter = new HashMap<>();
        for (CrmQuote quote : all) {
            String brand = StringUtils.hasText(quote.getBrand()) ? quote.getBrand().trim() : "未分类";
            counter.merge(brand, 1, Integer::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        counter.forEach((brand, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("brand", brand);
            item.put("count", count);
            rows.add(item);
        });
        rows.sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));
        return rows;
    }
}
