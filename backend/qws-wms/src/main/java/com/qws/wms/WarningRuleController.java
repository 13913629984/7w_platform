package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.ListResult;
import com.qws.common.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    public ApiResult<ListResult<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String scope,
                                                     @RequestParam(required = false) Integer enabled) {
        return ApiResult.ok(ListResult.of(warningRuleService.list(keyword, scope, enabled)));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(warningRuleService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    @Audit(module = "WMS", action = ActionType.CREATE, target = "预警规则")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(warningRuleService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/update")
    @Audit(module = "WMS", action = ActionType.UPDATE, target = "预警规则")
    public ApiResult<Map<String, Object>> update(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(warningRuleService.update(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/status")
    @Audit(module = "WMS", action = ActionType.STATUS, target = "预警规则状态")
    public ApiResult<Map<String, Object>> toggle(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(warningRuleService.toggle(request)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    @Audit(module = "WMS", action = ActionType.DELETE, target = "预警规则")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少规则ID");
        warningRuleService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
