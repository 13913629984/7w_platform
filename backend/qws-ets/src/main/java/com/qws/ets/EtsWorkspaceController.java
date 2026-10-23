package com.qws.ets;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.ets.EtsOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 电测-工作台接口 */
@RequirePermission("ETS")
@RestController
@RequestMapping("/ets/workspace")
public class EtsWorkspaceController {

    private final EtsWorkspaceService workspaceService;

    public EtsWorkspaceController(EtsWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/overview")
    public ApiResult<EtsOverviewVO> overview() {
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
