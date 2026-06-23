package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.dto.materialtype.MaterialTypeDeleteRequest;
import com.qws.common.dto.materialtype.MaterialTypeRequest;
import com.qws.common.dto.materialtype.MaterialTypeStatusRequest;
import com.qws.common.dto.materialtype.MaterialTypeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wms/material-type")
public class MaterialTypeController {

    private final MaterialTypeService service;

    public MaterialTypeController(MaterialTypeService service) {
        this.service = service;
    }

    @GetMapping("/tree")
    public ApiResult<List<MaterialTypeVO>> tree(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(service.tree(keyword));
    }

    @GetMapping("/list")
    public ApiResult<List<MaterialTypeVO>> list() {
        return ApiResult.ok(service.flatList());
    }

    @GetMapping("/{id}")
    public ApiResult<MaterialTypeVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(service.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<MaterialTypeVO> create(@RequestBody MaterialTypeRequest request) {
        try {
            return ApiResult.ok(service.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<MaterialTypeVO> update(@RequestBody MaterialTypeRequest request) {
        try {
            return ApiResult.ok(service.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody MaterialTypeDeleteRequest request) {
        try {
            service.delete(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/status")
    public ApiResult<MaterialTypeVO> changeStatus(@RequestBody MaterialTypeStatusRequest request) {
        try {
            return ApiResult.ok(service.changeStatus(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}