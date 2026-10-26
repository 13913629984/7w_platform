package com.qws.maint;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.maint.MaintListResult;
import com.qws.common.dto.maint.PlanDeleteRequest;
import com.qws.common.dto.maint.PlanRequest;
import com.qws.common.dto.maint.PlanVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 维保-保养计划接口 */
@RequirePermission("MAINT")
@RestController
@RequestMapping("/maint/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/list")
    public ApiResult<MaintListResult<PlanVO>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String cycleType,
                                                   @RequestParam(required = false) String equipmentKeyword,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(planService.list(keyword, status, cycleType, equipmentKeyword, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(planService.stats());
    }

    @GetMapping("/equipment-options")
    public ApiResult<List<Map<String, Object>>> equipmentOptions() {
        return ApiResult.ok(planService.equipmentOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<PlanVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(planService.detail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    @Audit(module = "MAINT", action = ActionType.CREATE, target = "保养计划")
    public ApiResult<PlanVO> create(@RequestBody PlanRequest request) {
        try {
            return ApiResult.ok(planService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    @Audit(module = "MAINT", action = ActionType.UPDATE, target = "保养计划")
    public ApiResult<PlanVO> update(@RequestBody PlanRequest request) {
        try {
            return ApiResult.ok(planService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    @Audit(module = "MAINT", action = ActionType.DELETE, target = "保养计划")
    public ApiResult<Void> delete(@RequestBody PlanDeleteRequest request) {
        try {
            planService.delete(request == null ? null : request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/execute")
    public ApiResult<PlanVO> execute(@RequestBody PlanRequest request) {
        try {
            return ApiResult.ok(planService.execute(request == null ? null : request.getId()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
