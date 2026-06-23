package com.qws.wms;

import com.qws.common.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wms")
public class WmsApplicationController {
    private final List<String> resources = List.of("overview", "materials", "types", "brands", "warehouses", "locations", "inventory", "inbound", "outbound", "stocktaking", "warnings", "purchase-demands", "purchase-orders");

    @GetMapping("/modules")
    public ApiResult<List<String>> modules() {
        return ApiResult.ok(resources);
    }

    @GetMapping("/{resource}")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable String resource, @RequestParam(required = false) String keyword) {
        return ApiResult.ok(List.of(
                Map.of("code", resource.toUpperCase() + "-001", "name", resource + " 示例一", "status", "启用", "updatedAt", LocalDateTime.now()),
                Map.of("code", resource.toUpperCase() + "-002", "name", resource + " 示例二", "status", "启用", "updatedAt", LocalDateTime.now())
        ));
    }

    @PostMapping("/{resource}")
    public ApiResult<Map<String, Object>> create(@PathVariable String resource, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(Map.of("resource", resource, "saved", true, "data", body));
    }
}
