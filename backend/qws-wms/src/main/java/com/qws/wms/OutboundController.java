package com.qws.wms;

import com.qws.common.ApiResult;
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
@RequestMapping("/wms/outbound")
public class OutboundController {

    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @GetMapping("/list")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String status) {
        return ApiResult.ok(outboundService.list(keyword, status));
    }

    @GetMapping("/details")
    public ApiResult<List<Map<String, Object>>> details(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String method) {
        return ApiResult.ok(outboundService.details(keyword, method));
    }

    @GetMapping("/sn")
    public ApiResult<List<Map<String, Object>>> sn(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status) {
        return ApiResult.ok(outboundService.snList(keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(outboundService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(outboundService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/execute")
    public ApiResult<Map<String, Object>> execute(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(outboundService.execute(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少出库单ID");
        outboundService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
