package com.qws.system;

import com.qws.common.ApiResult;
import com.qws.common.dto.audit.AuditLogCreateRequest;
import com.qws.common.dto.audit.AuditLogListResult;
import com.qws.common.dto.audit.AuditLogVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
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
}
