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
@RequestMapping("/wms/purchase-order")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping("/list")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String status) {
        return ApiResult.ok(purchaseOrderService.list(keyword, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(purchaseOrderService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(purchaseOrderService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(purchaseOrderService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/update")
    public ApiResult<Map<String, Object>> update(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(purchaseOrderService.update(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/confirm")
    public ApiResult<Map<String, Object>> confirm(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少采购订单ID");
        try { return ApiResult.ok(purchaseOrderService.confirm(number.longValue())); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create-inbound")
    public ApiResult<Map<String, Object>> createInbound(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少采购订单ID");
        try { return ApiResult.ok(purchaseOrderService.createInbound(number.longValue())); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/cancel")
    public ApiResult<Map<String, Object>> cancel(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少采购订单ID");
        try { return ApiResult.ok(purchaseOrderService.cancel(number.longValue())); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少采购订单ID");
        purchaseOrderService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
