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
@RequestMapping("/wms/purchase-demand")
public class PurchaseDemandController {

    private final PurchaseDemandService purchaseDemandService;

    public PurchaseDemandController(PurchaseDemandService purchaseDemandService) {
        this.purchaseDemandService = purchaseDemandService;
    }

    @GetMapping("/list")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String source,
                                                     @RequestParam(required = false) String status) {
        return ApiResult.ok(purchaseDemandService.list(keyword, source, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(purchaseDemandService.stats());
    }

    @GetMapping("/triggers")
    public ApiResult<List<Map<String, Object>>> triggers() {
        return ApiResult.ok(purchaseDemandService.triggers());
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(purchaseDemandService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(purchaseDemandService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/update")
    public ApiResult<Map<String, Object>> update(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(purchaseDemandService.update(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/generate-from-warning")
    public ApiResult<Map<String, Object>> generateFromWarning(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(purchaseDemandService.createFromTriggers(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/mrp")
    public ApiResult<Map<String, Object>> mrp() {
        try { return ApiResult.ok(purchaseDemandService.mrp()); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/generate-po")
    public ApiResult<Map<String, Object>> generatePO(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(purchaseDemandService.generatePO(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少需求ID");
        purchaseDemandService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
