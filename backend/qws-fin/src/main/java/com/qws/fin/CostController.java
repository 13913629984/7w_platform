package com.qws.fin;

import com.qws.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fin/cost")
public class CostController {

    private final CostService costService;

    public CostController(CostService costService) {
        this.costService = costService;
    }

    @GetMapping("/collections")
    public ApiResult<List<Map<String, Object>>> collections(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String costType) {
        return ApiResult.ok(costService.collections(keyword, costType));
    }

    @GetMapping("/pending-payments")
    public ApiResult<List<Map<String, Object>>> pendingPayments() {
        return ApiResult.ok(costService.pendingPayments());
    }

    @GetMapping("/allocations")
    public ApiResult<List<Map<String, Object>>> allocations(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String costType) {
        return ApiResult.ok(costService.allocations(keyword, costType));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(costService.stats());
    }

    @GetMapping("/charts")
    public ApiResult<Map<String, Object>> charts() {
        return ApiResult.ok(costService.charts());
    }

    @PostMapping("/collect")
    public ApiResult<Map<String, Object>> collect(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(costService.collect(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/allocate")
    public ApiResult<Map<String, Object>> allocate(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(costService.allocate(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }
}
