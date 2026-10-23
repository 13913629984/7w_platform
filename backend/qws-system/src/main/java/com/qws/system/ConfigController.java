package com.qws.system;

import com.qws.common.ApiResult;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.config.ConfigDeleteRequest;
import com.qws.common.dto.config.ConfigRequest;
import com.qws.common.dto.config.ConfigVO;
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
@RequestMapping("/system/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/list")
    public ApiResult<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String group) {
        return ApiResult.ok(configService.list(keyword, group));
    }

    @GetMapping("/{id}")
    public ApiResult<ConfigVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(configService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<ConfigVO> create(@RequestBody ConfigRequest request) {
        try {
            return ApiResult.ok(configService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<ConfigVO> update(@RequestBody ConfigRequest request) {
        try {
            return ApiResult.ok(configService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/batch-save")
    public ApiResult<Integer> batchSave(@RequestBody List<ConfigRequest> requests) {
        try {
            return ApiResult.ok(configService.batchSave(requests));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody ConfigDeleteRequest request) {
        try {
            configService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
