package com.qws.maint;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.maint.EquipmentDeleteRequest;
import com.qws.common.dto.maint.EquipmentRequest;
import com.qws.common.dto.maint.EquipmentVO;
import com.qws.common.dto.maint.MaintListResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 维保-设备台账接口 */
@RequirePermission("MAINT")
@RestController
@RequestMapping("/maint/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/list")
    public ApiResult<MaintListResult<EquipmentVO>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) String customerName,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(equipmentService.list(keyword, status, category, customerName, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(equipmentService.stats());
    }

    @GetMapping("/options")
    public ApiResult<List<Map<String, Object>>> options() {
        return ApiResult.ok(equipmentService.options());
    }

    @GetMapping("/{id}")
    public ApiResult<EquipmentVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(equipmentService.detail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<EquipmentVO> create(@RequestBody EquipmentRequest request) {
        try {
            return ApiResult.ok(equipmentService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<EquipmentVO> update(@RequestBody EquipmentRequest request) {
        try {
            return ApiResult.ok(equipmentService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody EquipmentDeleteRequest request) {
        try {
            equipmentService.delete(request == null ? null : request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
