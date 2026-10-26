package com.qws.fin;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 财务工作台、应收应付总览、业务流程三个聚合视图的只读接口。
 */
@RequirePermission("FIN")
@RestController
@RequestMapping("/fin")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final BusinessRelationService businessRelationService;

    public WorkspaceController(WorkspaceService workspaceService,
                               BusinessRelationService businessRelationService) {
        this.workspaceService = workspaceService;
        this.businessRelationService = businessRelationService;
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

    /**
     * 跨模块单据关联明细（销售链 + 采购链）。
     *
     * <p>业务流程页原来用的是前端写死的假数据，这里给真实查询。
     */
    @GetMapping("/business-flow/relations")
    public ApiResult<List<Map<String, Object>>> businessFlowRelations(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(businessRelationService.relations(keyword));
    }

    /** 关联完整度统计：总数 / 单据齐全数 / 待补齐数 */
    @GetMapping("/business-flow/relations/completeness")
    public ApiResult<Map<String, Object>> completeness(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(businessRelationService.completeness(keyword));
    }
}
