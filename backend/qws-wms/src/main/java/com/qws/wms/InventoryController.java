package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.dto.inventory.InventoryDeleteRequest;
import com.qws.common.dto.inventory.InventoryRequest;
import com.qws.common.dto.inventory.InventoryStatusRequest;
import com.qws.common.dto.inventory.InventoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wms/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/list")
    public ApiResult<List<InventoryVO>> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String warehouseCode,
                                             @RequestParam(required = false) String locationCode,
                                             @RequestParam(required = false) String stockStatus,
                                             @RequestParam(required = false) Integer status) {
        return ApiResult.ok(inventoryService.list(keyword, warehouseCode, locationCode, stockStatus, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(inventoryService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<InventoryVO> detail(@PathVariable Long id) {
        try { return ApiResult.ok(inventoryService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<InventoryVO> create(@RequestBody InventoryRequest request) {
        try { return ApiResult.ok(inventoryService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/update")
    public ApiResult<InventoryVO> update(@RequestBody InventoryRequest request) {
        try { return ApiResult.ok(inventoryService.update(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody InventoryDeleteRequest request) {
        try { inventoryService.delete(request.getId()); return ApiResult.ok(null); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/status")
    public ApiResult<InventoryVO> status(@RequestBody InventoryStatusRequest request) {
        try { return ApiResult.ok(inventoryService.changeStatus(request)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }
}