package com.qws.crm;

import com.qws.common.ApiResult;
import com.qws.common.dto.deal.DealBatchDeleteRequest;
import com.qws.common.dto.deal.DealDeleteRequest;
import com.qws.common.dto.deal.DealListResult;
import com.qws.common.dto.deal.DealRequest;
import com.qws.common.dto.deal.DealVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crm/deal")
public class DealController {

    private final CrmDealService dealService;

    public DealController(CrmDealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping("/list")
    public ApiResult<DealListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String owner,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(dealService.list(keyword, stage, owner, page, pageSize));
    }

    @GetMapping("/customers")
    public ApiResult<List<Map<String, Object>>> customers() {
        return ApiResult.ok(dealService.customerOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<DealVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(dealService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<DealVO> create(@RequestBody DealRequest request) {
        try {
            return ApiResult.ok(dealService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<DealVO> update(@RequestBody DealRequest request) {
        try {
            return ApiResult.ok(dealService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody DealDeleteRequest request) {
        try {
            dealService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public ApiResult<Integer> batchDelete(@RequestBody DealBatchDeleteRequest request) {
        return ApiResult.ok(dealService.batchDelete(request));
    }

    @PostMapping("/convert/{id}")
    public ApiResult<DealVO> convert(@PathVariable Long id) {
        try {
            return ApiResult.ok(dealService.convert(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
