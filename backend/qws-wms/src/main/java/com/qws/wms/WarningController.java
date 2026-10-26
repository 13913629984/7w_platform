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

import java.util.List;
import java.util.Map;

@RequirePermission("WMS")
@RestController
@RequestMapping("/wms/warning")
public class WarningController {

    private final WarningService warningService;

    public WarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    @GetMapping("/list")
    public ApiResult<ListResult<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String level,
                                                     @RequestParam(required = false) String status) {
        return ApiResult.ok(ListResult.of(warningService.list(keyword, level, status)));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(warningService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(warningService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/scan")
    @Audit(module = "WMS", action = ActionType.EXECUTE, target = "预警扫描")
    public ApiResult<Map<String, Object>> scan() {
        return ApiResult.ok(warningService.scan());
    }

    @PostMapping("/handle")
    @Audit(module = "WMS", action = ActionType.UPDATE, target = "预警处理")
    public ApiResult<Map<String, Object>> handle(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(warningService.handle(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    @Audit(module = "WMS", action = ActionType.DELETE, target = "预警")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少预警ID");
        warningService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
