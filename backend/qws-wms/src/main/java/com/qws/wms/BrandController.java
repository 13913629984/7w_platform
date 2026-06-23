package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.dto.brand.BrandDeleteRequest;
import com.qws.common.dto.brand.BrandRequest;
import com.qws.common.dto.brand.BrandStatusRequest;
import com.qws.common.dto.brand.BrandVO;
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
@RequestMapping("/wms/brand")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/list")
    public ApiResult<List<BrandVO>> list(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status) {
        return ApiResult.ok(brandService.list(keyword, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(brandService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<BrandVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(brandService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<BrandVO> create(@RequestBody BrandRequest request) {
        try {
            return ApiResult.ok(brandService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<BrandVO> update(@RequestBody BrandRequest request) {
        try {
            return ApiResult.ok(brandService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody BrandDeleteRequest request) {
        try {
            brandService.delete(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/status")
    public ApiResult<BrandVO> changeStatus(@RequestBody BrandStatusRequest request) {
        try {
            return ApiResult.ok(brandService.changeStatus(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}