package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.dto.bom.BomDeleteRequest;
import com.qws.common.dto.bom.BomNodeMaterialRequest;
import com.qws.common.dto.bom.BomNodeMaterialVO;
import com.qws.common.dto.bom.BomNodeRequest;
import com.qws.common.dto.bom.BomNodeVO;
import com.qws.common.dto.bom.BomProductListResult;
import com.qws.common.dto.bom.BomProductRequest;
import com.qws.common.dto.bom.BomProductVO;
import com.qws.common.dto.bom.BomStatusRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wms/bom")
public class BomController {

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @GetMapping("/products")
    public ApiResult<BomProductListResult> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResult.ok(bomService.listProducts(keyword, status, page, pageSize));
    }

    @GetMapping("/products/{id}")
    public ApiResult<BomProductVO> getProduct(@PathVariable Long id) {
        try {
            return ApiResult.ok(bomService.getProduct(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/products/create")
    public ApiResult<BomProductVO> createProduct(@RequestBody BomProductRequest request) {
        try {
            return ApiResult.ok(bomService.createProduct(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/products/update")
    public ApiResult<BomProductVO> updateProduct(@RequestBody BomProductRequest request) {
        try {
            return ApiResult.ok(bomService.updateProduct(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/products/delete")
    public ApiResult<Void> deleteProduct(@RequestBody BomDeleteRequest request) {
        try {
            bomService.deleteProduct(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/products/status")
    public ApiResult<BomProductVO> changeProductStatus(@RequestBody BomStatusRequest request) {
        try {
            return ApiResult.ok(bomService.changeProductStatus(request.getId(), request.getStatus()));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/nodes/tree")
    public ApiResult<List<BomNodeVO>> tree(@RequestParam Long productId) {
        try {
            return ApiResult.ok(bomService.tree(productId));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/nodes/save")
    public ApiResult<BomNodeVO> saveNode(@RequestBody BomNodeRequest request) {
        try {
            return ApiResult.ok(bomService.saveNode(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/nodes/delete")
    public ApiResult<Void> deleteNode(@RequestBody BomDeleteRequest request) {
        try {
            bomService.deleteNode(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/nodes/materials/save")
    public ApiResult<BomNodeMaterialVO> addNodeMaterial(@RequestBody BomNodeMaterialRequest request) {
        try {
            return ApiResult.ok(bomService.addNodeMaterial(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/nodes/materials/delete")
    public ApiResult<Void> removeNodeMaterial(@RequestBody BomDeleteRequest request) {
        try {
            bomService.removeNodeMaterial(request.getId());
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
