package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.supplier.SupplierDeleteRequest;
import com.qws.common.dto.supplier.SupplierRequest;
import com.qws.common.dto.supplier.SupplierStatusRequest;
import com.qws.common.dto.supplier.SupplierVO;
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
@RequestMapping("/wms/supplier")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/list")
    public ApiResult<List<SupplierVO>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer status) {
        return ApiResult.ok(supplierService.list(keyword, category, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(supplierService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<SupplierVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(supplierService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    @Audit(module = "WMS", action = ActionType.CREATE, target = "供应商")
    public ApiResult<SupplierVO> create(@RequestBody SupplierRequest request) {
        try {
            return ApiResult.ok(supplierService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    @Audit(module = "WMS", action = ActionType.UPDATE, target = "供应商")
    public ApiResult<SupplierVO> update(@RequestBody SupplierRequest request) {
        try {
            return ApiResult.ok(supplierService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    @Audit(module = "WMS", action = ActionType.DELETE, target = "供应商")
    public ApiResult<Void> delete(@RequestBody SupplierDeleteRequest request) {
        try {
            supplierService.delete(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/status")
    public ApiResult<SupplierVO> changeStatus(@RequestBody SupplierStatusRequest request) {
        try {
            return ApiResult.ok(supplierService.changeStatus(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
