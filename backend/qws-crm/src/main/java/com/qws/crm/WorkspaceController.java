package com.qws.crm;

import com.qws.common.ApiResult;
import com.qws.common.dto.workspace.WorkspaceOverview;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/workspace")
public class WorkspaceController {

    private final CrmWorkspaceService workspaceService;

    public WorkspaceController(CrmWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/overview")
    public ApiResult<WorkspaceOverview> overview() {
        return ApiResult.ok(workspaceService.overview());
    }
}
