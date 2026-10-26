package com.qws.system;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.audit.AuditLogCreateRequest;
import com.qws.common.dto.audit.AuditLogListResult;
import com.qws.common.dto.audit.AuditLogVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequirePermission("SYS")
@RestController
@RequestMapping("/system/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditLogRetentionService retentionService;

    public AuditLogController(AuditLogService auditLogService,
                              AuditLogRetentionService retentionService) {
        this.auditLogService = auditLogService;
        this.retentionService = retentionService;
    }

    @GetMapping("/list")
    public ApiResult<AuditLogListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(auditLogService.list(keyword, moduleCode, actionType, status, startDate, endDate, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<AuditLogVO> detail(@PathVariable Long id) {
        AuditLogVO detail = auditLogService.detail(id);
        return detail == null ? ApiResult.fail("日志不存在") : ApiResult.ok(detail);
    }

    @PostMapping("/record")
    public ApiResult<AuditLogVO> record(@RequestBody AuditLogCreateRequest request) {
        return ApiResult.ok(auditLogService.create(request));
    }

    /**
     * 审计日志保留策略（供运维/前端展示：保留多少天、当前总量、失败记录是否保留）。
     */
    @GetMapping("/retention/policy")
    public ApiResult<Map<String, Object>> retentionPolicy() {
        return ApiResult.ok(retentionService.policy());
    }

    /**
     * 手动触发一次清理（默认按策略里的保留天数）。
     *
     * <p>日常由定时任务在凌晨自动跑，这个接口用于运维应急（如磁盘告警）
     * 或验证策略配置是否生效。
     */
    @PostMapping("/retention/cleanup")
    @Audit(module = "SYS", action = ActionType.DELETE, target = "审计日志清理")
    public ApiResult<Map<String, Object>> cleanup(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit) {
        Map<String, Object> policy = retentionService.policy();
        int effectiveDays = days != null ? days : ((Number) policy.get("retentionDays")).intValue();
        int effectiveLimit = limit != null ? limit : ((Number) policy.get("maxDeletePerRun")).intValue();
        try {
            return ApiResult.ok(retentionService.cleanup(effectiveDays, effectiveLimit));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
