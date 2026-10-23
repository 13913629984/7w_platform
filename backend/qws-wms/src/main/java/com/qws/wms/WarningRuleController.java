package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequirePermission("WMS")
@RestController
@RequestMapping("/wms/warning/rule")
public class WarningRuleController {

    private final WarningRuleService warningRuleService;

    public WarningRuleController(WarningRuleService warningRuleService) {
        this.warningRuleService = warningRuleService;
    }

    @GetMapping("/list")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String scope,
                                                     @RequestParam(required = false) Integer enabled) {
        return ApiResult.ok(warningRuleService.list(keyword, scope, enabled));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(warningRuleService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(warningRuleService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/update")
    public ApiResult<Map<String, Object>> update(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(warningRuleService.update(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/status")
    public ApiResult<Map<String, Object>> toggle(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(warningRuleService.toggle(request)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少规则ID");
        warningRuleService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
