package com.qws.wms;

import com.qws.common.ApiResult;
import com.qws.common.dto.location.LocationDeleteRequest;
import com.qws.common.dto.location.LocationRequest;
import com.qws.common.dto.location.LocationStatusRequest;
import com.qws.common.dto.location.LocationVO;
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
@RequestMapping("/wms/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/list")
    public ApiResult<List<LocationVO>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String warehouseCode,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) Integer status) {
        return ApiResult.ok(locationService.list(keyword, warehouseCode, type, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(locationService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<LocationVO> detail(@PathVariable Long id) {
        try { return ApiResult.ok(locationService.detail(id)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/create")
    public ApiResult<LocationVO> create(@RequestBody LocationRequest request) {
        try { return ApiResult.ok(locationService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/update")
    public ApiResult<LocationVO> update(@RequestBody LocationRequest request) {
        try { return ApiResult.ok(locationService.update(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody LocationDeleteRequest request) {
        try { locationService.delete(request.getId()); return ApiResult.ok(null); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/status")
    public ApiResult<LocationVO> status(@RequestBody LocationStatusRequest request) {
        try { return ApiResult.ok(locationService.changeStatus(request)); }
        catch (IllegalArgumentException ex) { return ApiResult.fail(ex.getMessage()); }
    }
}