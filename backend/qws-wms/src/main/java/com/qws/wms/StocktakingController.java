package com.qws.wms;

import com.qws.common.ApiResult;
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
@RequestMapping("/wms/stocktaking")
public class StocktakingController {

    private final StocktakingService stocktakingService;

    public StocktakingController(StocktakingService stocktakingService) {
        this.stocktakingService = stocktakingService;
    }

    @GetMapping("/list")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String status) {
        return ApiResult.ok(stocktakingService.list(keyword, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(stocktakingService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(stocktakingService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(stocktakingService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/start")
    public ApiResult<Map<String, Object>> start(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少盘点单ID");
        try { return ApiResult.ok(stocktakingService.start(number.longValue())); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/process")
    public ApiResult<Map<String, Object>> process(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(stocktakingService.process(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/review")
    public ApiResult<Map<String, Object>> review(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(stocktakingService.review(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/complete")
    public ApiResult<Map<String, Object>> complete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少盘点单ID");
        try { return ApiResult.ok(stocktakingService.complete(number.longValue())); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少盘点单ID");
        stocktakingService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
