package com.qws.ets;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.ets.RegisterDeleteRequest;
import com.qws.common.dto.ets.RegisterListResult;
import com.qws.common.dto.ets.RegisterRequest;
import com.qws.common.dto.ets.RegisterVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 电测-来货登记接口 */
@RequirePermission("ETS")
@RestController
@RequestMapping("/ets/register")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @GetMapping("/list")
    public ApiResult<RegisterListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            return ApiResult.ok(registerService.list(keyword, status, customerName, page, pageSize));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        try {
            return ApiResult.ok(registerService.stats());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/options")
    public ApiResult<List<Map<String, Object>>> options() {
        try {
            return ApiResult.ok(registerService.options());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResult<RegisterVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(registerService.detail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<RegisterVO> create(@RequestBody RegisterRequest request) {
        try {
            return ApiResult.ok(registerService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<RegisterVO> update(@RequestBody RegisterRequest request) {
        try {
            return ApiResult.ok(registerService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody RegisterDeleteRequest request) {
        try {
            registerService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
