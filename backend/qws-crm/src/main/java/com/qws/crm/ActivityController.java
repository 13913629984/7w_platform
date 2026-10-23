package com.qws.crm;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.activity.ActivityDeleteRequest;
import com.qws.common.dto.activity.ActivityListResult;
import com.qws.common.dto.activity.ActivityRequest;
import com.qws.common.dto.activity.ActivityVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequirePermission("CRM")
@RestController
@RequestMapping("/crm/activity")
public class ActivityController {

    private final CrmActivityService activityService;

    public ActivityController(CrmActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/list")
    public ApiResult<ActivityListResult> list(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) Long customerId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(activityService.list(keyword, status, type, customerId, page, pageSize));
    }

    @GetMapping("/customers")
    public ApiResult<List<Map<String, Object>>> customers() {
        return ApiResult.ok(activityService.customerOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<ActivityVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(activityService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<ActivityVO> create(@RequestBody ActivityRequest request) {
        try {
            return ApiResult.ok(activityService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<ActivityVO> update(@RequestBody ActivityRequest request) {
        try {
            return ApiResult.ok(activityService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody ActivityDeleteRequest request) {
        try {
            activityService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
