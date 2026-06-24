package com.qws.fin;

import com.qws.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 财务工作台、应收应付总览、业务流程三个聚合视图的只读接口。
 */
@RestController
@RequestMapping("/fin")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/workspace")
    public ApiResult<Map<String, Object>> workspace() {
        return ApiResult.ok(workspaceService.workspace());
    }

    @GetMapping("/arap")
    public ApiResult<Map<String, Object>> arap(@RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResult.ok(workspaceService.arap(keyword, page, pageSize));
    }

    @GetMapping("/business-flow/list")
    public ApiResult<List<Map<String, Object>>> businessFlow(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(workspaceService.businessFlow(keyword));
    }

    @GetMapping("/business-flow/stats")
    public ApiResult<Map<String, Object>> businessFlowStats() {
        return ApiResult.ok(workspaceService.businessFlowStats());
    }
}
