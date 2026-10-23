package com.qws.system;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.dict.DictDeleteRequest;
import com.qws.common.dto.dict.DictItemRequest;
import com.qws.common.dto.dict.DictItemVO;
import com.qws.common.dto.dict.DictTypeRequest;
import com.qws.common.dto.dict.DictTypeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequirePermission("SYS")
@RestController
@RequestMapping("/system/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/types")
    public ApiResult<Map<String, Object>> types(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        return ApiResult.ok(dictService.listTypes(keyword, status));
    }

    @GetMapping("/items/{dictCode}")
    public ApiResult<List<DictItemVO>> items(@PathVariable String dictCode) {
        try {
            return ApiResult.ok(dictService.listItems(dictCode));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/type/create")
    public ApiResult<DictTypeVO> createType(@RequestBody DictTypeRequest request) {
        try {
            return ApiResult.ok(dictService.createType(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/type/update")
    public ApiResult<DictTypeVO> updateType(@RequestBody DictTypeRequest request) {
        try {
            return ApiResult.ok(dictService.updateType(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/type/delete")
    public ApiResult<Void> deleteType(@RequestBody DictDeleteRequest request) {
        try {
            dictService.deleteType(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/item/create")
    public ApiResult<DictItemVO> createItem(@RequestBody DictItemRequest request) {
        try {
            return ApiResult.ok(dictService.createItem(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/item/update")
    public ApiResult<DictItemVO> updateItem(@RequestBody DictItemRequest request) {
        try {
            return ApiResult.ok(dictService.updateItem(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/item/delete")
    public ApiResult<Void> deleteItem(@RequestBody DictDeleteRequest request) {
        try {
            dictService.deleteItem(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
