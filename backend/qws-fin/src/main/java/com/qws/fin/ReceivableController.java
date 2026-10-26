package com.qws.fin;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequirePermission("FIN")
@RestController
@RequestMapping("/fin/receivable")
public class ReceivableController {

    private final ReceivableService receivableService;

    public ReceivableController(ReceivableService receivableService) {
        this.receivableService = receivableService;
    }

    @GetMapping("/list")
    public ApiResult<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String customer,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResult.ok(receivableService.list(keyword, status, customer, page, pageSize));
    }

    @GetMapping("/records")
    public ApiResult<Map<String, Object>> records(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResult.ok(receivableService.records(keyword, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(receivableService.stats());
    }

    @PostMapping("/create")
    @Audit(module = "FIN", action = ActionType.CREATE, target = "应收款")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(receivableService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/write-off")
    @Audit(module = "FIN", action = ActionType.EXECUTE, target = "回款核销")
    public ApiResult<Map<String, Object>> writeOff(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(receivableService.writeOff(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/invoice")
    @Audit(module = "FIN", action = ActionType.UPDATE, target = "应收开票")
    public ApiResult<Map<String, Object>> invoice(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(receivableService.invoice(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }
}
