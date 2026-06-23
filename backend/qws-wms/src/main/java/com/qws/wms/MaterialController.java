package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.dto.material.MaterialBatchDeleteRequest;
import com.qws.common.dto.material.MaterialDeleteRequest;
import com.qws.common.dto.material.MaterialImportRequest;
import com.qws.common.dto.material.MaterialImportResult;
import com.qws.common.dto.material.MaterialListResult;
import com.qws.common.dto.material.MaterialRequest;
import com.qws.common.dto.material.MaterialStatusRequest;
import com.qws.common.dto.material.MaterialVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/material")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/list")
    public ApiResult<MaterialListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(materialService.list(keyword, category, status, page, pageSize));
    }

    @GetMapping("/export")
    public ApiResult<MaterialListResult> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        return ApiResult.ok(materialService.export(keyword, category, status));
    }

    @GetMapping("/{id}")
    public ApiResult<MaterialVO> detail(@PathVariable Long id) {
        return ApiResult.ok(materialService.detail(id));
    }

    @PostMapping("/create")
    public ApiResult<MaterialVO> create(@RequestBody MaterialRequest request) {
        try {
            return ApiResult.ok(materialService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<MaterialVO> update(@RequestBody MaterialRequest request) {
        try {
            return ApiResult.ok(materialService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody MaterialDeleteRequest request) {
        try {
            materialService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public ApiResult<Integer> batchDelete(@RequestBody MaterialBatchDeleteRequest request) {
        return ApiResult.ok(materialService.batchDelete(request));
    }

    @PostMapping("/status")
    public ApiResult<MaterialVO> changeStatus(@RequestBody MaterialStatusRequest request) {
        try {
            return ApiResult.ok(materialService.changeStatus(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/import")
    public ApiResult<MaterialImportResult> importBatch(@RequestBody MaterialImportRequest request) {
        return ApiResult.ok(materialService.importBatch(request));
    }
}
