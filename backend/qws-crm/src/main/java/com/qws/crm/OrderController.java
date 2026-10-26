package com.qws.crm;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import com.qws.common.security.RequirePermission;
import com.qws.common.dto.order.OrderBatchDeleteRequest;
import com.qws.common.dto.order.OrderDeleteRequest;
import com.qws.common.dto.order.OrderListResult;
import com.qws.common.dto.order.OrderRequest;
import com.qws.common.dto.order.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequirePermission("CRM")
@RestController
@RequestMapping("/crm/order")
public class OrderController {

    private final CrmOrderService orderService;

    public OrderController(CrmOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/list")
    public ApiResult<OrderListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(orderService.list(keyword, status, customerId, page, pageSize));
    }

    @GetMapping("/next-no")
    public ApiResult<String> nextNo() {
        return ApiResult.ok(orderService.nextOrderNo());
    }

    @GetMapping("/customers")
    public ApiResult<List<Map<String, Object>>> customers() {
        return ApiResult.ok(orderService.customerOptions());
    }

    @GetMapping("/deals")
    public ApiResult<List<Map<String, Object>>> deals() {
        return ApiResult.ok(orderService.dealOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<OrderVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(orderService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    @Audit(module = "CRM", action = ActionType.CREATE, target = "销售订单")
    public ApiResult<OrderVO> create(@RequestBody OrderRequest request) {
        try {
            return ApiResult.ok(orderService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    @Audit(module = "CRM", action = ActionType.UPDATE, target = "销售订单")
    public ApiResult<OrderVO> update(@RequestBody OrderRequest request) {
        try {
            return ApiResult.ok(orderService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    @Audit(module = "CRM", action = ActionType.DELETE, target = "销售订单")
    public ApiResult<Void> delete(@RequestBody OrderDeleteRequest request) {
        try {
            orderService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    @Audit(module = "CRM", action = ActionType.DELETE, target = "销售订单")
    public ApiResult<Integer> batchDelete(@RequestBody OrderBatchDeleteRequest request) {
        return ApiResult.ok(orderService.batchDelete(request));
    }

    @PostMapping("/confirm/{id}")
    @Audit(module = "CRM", action = ActionType.STATUS, target = "销售订单确认")
    public ApiResult<OrderVO> confirm(@PathVariable Long id) {
        try {
            return ApiResult.ok(orderService.confirm(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/deliver/{id}")
    @Audit(module = "CRM", action = ActionType.EXECUTE, target = "销售订单发货")
    public ApiResult<OrderVO> deliver(@PathVariable Long id) {
        try {
            return ApiResult.ok(orderService.deliver(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }
}
