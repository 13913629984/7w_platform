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
@RequestMapping("/wms/inbound")
public class InboundController {

    private final InboundService inboundService;

    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @GetMapping("/list")
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String status) {
        return ApiResult.ok(inboundService.list(keyword, status));
    }

    @GetMapping("/details")
    public ApiResult<List<Map<String, Object>>> details(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String method) {
        return ApiResult.ok(inboundService.details(keyword, method));
    }

    @GetMapping("/sn")
    public ApiResult<List<Map<String, Object>>> sn(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status) {
        return ApiResult.ok(inboundService.snList(keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        try { return ApiResult.ok(inboundService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(inboundService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/execute")
    public ApiResult<Map<String, Object>> execute(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(inboundService.execute(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody Map<String, Object> request) {
        Object id = request.get("id");
        if (!(id instanceof Number number)) return ApiResult.fail("缺少入库单ID");
        inboundService.delete(number.longValue());
        return ApiResult.ok(null);
    }
}
