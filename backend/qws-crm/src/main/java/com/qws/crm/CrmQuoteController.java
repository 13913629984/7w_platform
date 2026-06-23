package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.ApiResult;
import com.qws.common.entity.CrmQuote;
import com.qws.common.mapper.CrmQuoteMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/crm/quote")
public class CrmQuoteController {

    private final CrmQuoteMapper quoteMapper;

    public CrmQuoteController(CrmQuoteMapper quoteMapper) {
        this.quoteMapper = quoteMapper;
    }

    @GetMapping("/list")
    public ApiResult<Map<String, Object>> list(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        LambdaQueryWrapper<CrmQuote> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(customerName)) {
            wrapper.like(CrmQuote::getCustomerName, customerName.trim());
        }
        if (StringUtils.hasText(brand)) {
            wrapper.like(CrmQuote::getBrand, brand.trim());
        }
        if (StringUtils.hasText(model)) {
            wrapper.like(CrmQuote::getModel, model.trim());
        }
        wrapper.orderByAsc(CrmQuote::getId);

        Page<CrmQuote> result = quoteMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("rows", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return ApiResult.ok(data);
    }
}
