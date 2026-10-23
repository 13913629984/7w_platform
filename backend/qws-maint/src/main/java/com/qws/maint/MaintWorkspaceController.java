package com.qws.maint;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.maint.MaintOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 维保-工作台接口 */
@RequirePermission("MAINT")
@RestController
@RequestMapping("/maint/workspace")
public class MaintWorkspaceController {

    private final MaintWorkspaceService workspaceService;

    public MaintWorkspaceController(MaintWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/overview")
    public ApiResult<MaintOverviewVO> overview() {
        try {
            return ApiResult.ok(workspaceService.overview());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/trend")
    public ApiResult<List<Map<String, Object>>> trend() {
        try {
            return ApiResult.ok(workspaceService.trend());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/distribution")
    public ApiResult<List<Map<String, Object>>> distribution() {
        try {
            return ApiResult.ok(workspaceService.distribution());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
