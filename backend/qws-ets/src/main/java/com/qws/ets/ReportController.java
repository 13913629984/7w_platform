package com.qws.ets;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.ets.ReportApproveRequest;
import com.qws.common.dto.ets.ReportDeleteRequest;
import com.qws.common.dto.ets.ReportListResult;
import com.qws.common.dto.ets.ReportRequest;
import com.qws.common.dto.ets.ReportVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 电测-测试报告接口 */
@RequirePermission("ETS")
@RestController
@RequestMapping("/ets/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/list")
    public ApiResult<ReportListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            return ApiResult.ok(reportService.list(keyword, status, customerName, page, pageSize));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        try {
            return ApiResult.ok(reportService.stats());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/register-options")
    public ApiResult<List<Map<String, Object>>> registerOptions() {
        try {
            return ApiResult.ok(reportService.registerOptions());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResult<ReportVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(reportService.detail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<ReportVO> create(@RequestBody ReportRequest request) {
        try {
            return ApiResult.ok(reportService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<ReportVO> update(@RequestBody ReportRequest request) {
        try {
            return ApiResult.ok(reportService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody ReportDeleteRequest request) {
        try {
            reportService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/approve")
    public ApiResult<ReportVO> approve(@RequestBody ReportApproveRequest request) {
        try {
            return ApiResult.ok(reportService.approve(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/issue")
    public ApiResult<ReportVO> issue(@RequestParam(required = false) Long id,
                                     @RequestBody(required = false) ReportDeleteRequest request) {
        try {
            Long target = id != null ? id : (request == null ? null : request.getId());
            if (target == null) {
                throw new IllegalArgumentException("请选择要签发的测试报告");
            }
            return ApiResult.ok(reportService.issue(target));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
