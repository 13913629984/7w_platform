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
@RequestMapping("/fin/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/list")
    public ApiResult<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResult.ok(paymentService.list(keyword, status, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(paymentService.stats());
    }

    @PostMapping("/create")
    @Audit(module = "FIN", action = ActionType.CREATE, target = "付款单")
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(paymentService.create(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/approve")
    @Audit(module = "FIN", action = ActionType.APPROVE, target = "付款审批")
    public ApiResult<Map<String, Object>> approve(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(paymentService.approve(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }

    @PostMapping("/reject")
    @Audit(module = "FIN", action = ActionType.APPROVE, target = "付款驳回")
    public ApiResult<Map<String, Object>> reject(@RequestBody Map<String, Object> request) {
        try { return ApiResult.ok(paymentService.reject(request)); }
        catch (IllegalArgumentException | IllegalStateException ex) { return ApiResult.fail(ex.getMessage()); }
    }
}
