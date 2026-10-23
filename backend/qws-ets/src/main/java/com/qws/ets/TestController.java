package com.qws.ets;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.ets.TestDeleteRequest;
import com.qws.common.dto.ets.TestListResult;
import com.qws.common.dto.ets.TestRequest;
import com.qws.common.dto.ets.TestResultRequest;
import com.qws.common.dto.ets.TestVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 电测-测试执行接口 */
@RequirePermission("ETS")
@RestController
@RequestMapping("/ets/test")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/list")
    public ApiResult<TestListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String registerNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            return ApiResult.ok(testService.list(keyword, result, category, registerNo, page, pageSize));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        try {
            return ApiResult.ok(testService.stats());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/register-options")
    public ApiResult<List<Map<String, Object>>> registerOptions() {
        try {
            return ApiResult.ok(testService.registerOptions());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResult<TestVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(testService.detail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<TestVO> create(@RequestBody TestRequest request) {
        try {
            return ApiResult.ok(testService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<TestVO> update(@RequestBody TestRequest request) {
        try {
            return ApiResult.ok(testService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody TestDeleteRequest request) {
        try {
            testService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/result")
    public ApiResult<TestVO> result(@RequestBody TestResultRequest request) {
        try {
            return ApiResult.ok(testService.submitResult(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
