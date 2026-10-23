package com.qws.crm;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.lead.LeadConvertRequest;
import com.qws.common.dto.lead.LeadDeleteRequest;
import com.qws.common.dto.lead.LeadListResult;
import com.qws.common.dto.lead.LeadRequest;
import com.qws.common.dto.lead.LeadVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequirePermission("CRM")
@RestController
@RequestMapping("/crm/lead")
public class LeadController {

    private final CrmLeadService leadService;

    public LeadController(CrmLeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping("/list")
    public ApiResult<LeadListResult> list(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String source,
                                          @RequestParam(required = false) String owner,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(leadService.list(keyword, status, source, owner, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<LeadVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(leadService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<LeadVO> create(@RequestBody LeadRequest request) {
        try {
            return ApiResult.ok(leadService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<LeadVO> update(@RequestBody LeadRequest request) {
        try {
            return ApiResult.ok(leadService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody LeadDeleteRequest request) {
        try {
            leadService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/convert")
    public ApiResult<Long> convert(@RequestBody LeadConvertRequest request) {
        try {
            return ApiResult.ok(leadService.convert(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
