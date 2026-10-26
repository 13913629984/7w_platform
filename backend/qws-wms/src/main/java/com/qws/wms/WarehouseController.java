package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.warehouse.WarehouseDeleteRequest;
import com.qws.common.dto.warehouse.WarehouseRequest;
import com.qws.common.dto.warehouse.WarehouseStatusRequest;
import com.qws.common.dto.warehouse.WarehouseVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequirePermission("WMS")
@RestController
@RequestMapping("/wms/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/list")
    public ApiResult<List<WarehouseVO>> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Integer status) {
        return ApiResult.ok(warehouseService.list(keyword, type, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(warehouseService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<WarehouseVO> detail(@PathVariable Long id) {
        try { return ApiResult.ok(warehouseService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    @Audit(module = "WMS", action = ActionType.CREATE, target = "仓库")
    public ApiResult<WarehouseVO> create(@RequestBody WarehouseRequest request) {
        try { return ApiResult.ok(warehouseService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/update")
    @Audit(module = "WMS", action = ActionType.UPDATE, target = "仓库")
    public ApiResult<WarehouseVO> update(@RequestBody WarehouseRequest request) {
        try { return ApiResult.ok(warehouseService.update(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    @Audit(module = "WMS", action = ActionType.DELETE, target = "仓库")
    public ApiResult<Void> delete(@RequestBody WarehouseDeleteRequest request) {
        try { warehouseService.delete(request.getId()); return ApiResult.ok(null); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/status")
    public ApiResult<WarehouseVO> status(@RequestBody WarehouseStatusRequest request) {
        try { return ApiResult.ok(warehouseService.changeStatus(request)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }
}