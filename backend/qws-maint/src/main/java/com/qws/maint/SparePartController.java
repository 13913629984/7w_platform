package com.qws.maint;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.maint.MaintListResult;
import com.qws.common.dto.maint.SparePartDeleteRequest;
import com.qws.common.dto.maint.SparePartRequest;
import com.qws.common.dto.maint.SparePartVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 维保-备件管理接口 */
@RequirePermission("MAINT")
@RestController
@RequestMapping("/maint/spare-part")
public class SparePartController {

    private final SparePartService sparePartService;

    public SparePartController(SparePartService sparePartService) {
        this.sparePartService = sparePartService;
    }

    @GetMapping("/list")
    public ApiResult<MaintListResult<SparePartVO>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) String warehouseCode,
                                                        @RequestParam(required = false) Boolean lowStockOnly,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(sparePartService.list(keyword, category, warehouseCode, lowStockOnly, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(sparePartService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<SparePartVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(sparePartService.detail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    @Audit(module = "MAINT", action = ActionType.CREATE, target = "备件")
    public ApiResult<SparePartVO> create(@RequestBody SparePartRequest request) {
        try {
            return ApiResult.ok(sparePartService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    @Audit(module = "MAINT", action = ActionType.UPDATE, target = "备件")
    public ApiResult<SparePartVO> update(@RequestBody SparePartRequest request) {
        try {
            return ApiResult.ok(sparePartService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    @Audit(module = "MAINT", action = ActionType.DELETE, target = "备件")
    public ApiResult<Void> delete(@RequestBody SparePartDeleteRequest request) {
        try {
            sparePartService.delete(request == null ? null : request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
