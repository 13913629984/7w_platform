package com.qws.maint;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.maint.MaintListResult;
import com.qws.common.dto.maint.WorkOrderDeleteRequest;
import com.qws.common.dto.maint.WorkOrderRequest;
import com.qws.common.dto.maint.WorkOrderStatusRequest;
import com.qws.common.dto.maint.WorkOrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 维保-维修工单接口 */
@RequirePermission("MAINT")
@RestController
@RequestMapping("/maint/work-order")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping("/list")
    public ApiResult<MaintListResult<WorkOrderVO>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String priority,
                                                        @RequestParam(required = false) String type,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(workOrderService.list(keyword, status, priority, type, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(workOrderService.stats());
    }

    @GetMapping("/equipment-options")
    public ApiResult<List<Map<String, Object>>> equipmentOptions() {
        return ApiResult.ok(workOrderService.equipmentOptions());
    }

    @GetMapping("/handler-options")
    public ApiResult<List<Map<String, Object>>> handlerOptions() {
        return ApiResult.ok(workOrderService.handlerOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<WorkOrderVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(workOrderService.detail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    @Audit(module = "MAINT", action = ActionType.CREATE, target = "维保工单")
    public ApiResult<WorkOrderVO> create(@RequestBody WorkOrderRequest request) {
        try {
            return ApiResult.ok(workOrderService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    @Audit(module = "MAINT", action = ActionType.UPDATE, target = "维保工单")
    public ApiResult<WorkOrderVO> update(@RequestBody WorkOrderRequest request) {
        try {
            return ApiResult.ok(workOrderService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    @Audit(module = "MAINT", action = ActionType.DELETE, target = "维保工单")
    public ApiResult<Void> delete(@RequestBody WorkOrderDeleteRequest request) {
        try {
            workOrderService.delete(request == null ? null : request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/status")
    public ApiResult<WorkOrderVO> changeStatus(@RequestBody WorkOrderStatusRequest request) {
        try {
            return ApiResult.ok(workOrderService.changeStatus(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
