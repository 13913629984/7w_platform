package com.qws.crm;

import com.qws.common.ApiResult;
import com.qws.common.dto.customer.CustomerBatchDeleteRequest;
import com.qws.common.dto.customer.CustomerDeleteRequest;
import com.qws.common.dto.customer.CustomerImportRequest;
import com.qws.common.dto.customer.CustomerImportResult;
import com.qws.common.dto.customer.CustomerListResult;
import com.qws.common.dto.customer.CustomerRequest;
import com.qws.common.dto.customer.CustomerTransferRequest;
import com.qws.common.dto.customer.CustomerVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/customer")
public class CustomerController {

    private final CrmCustomerService customerService;

    public CustomerController(CrmCustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/list")
    public ApiResult<CustomerListResult> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false, defaultValue = "false") boolean onlyPublic,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(customerService.list(name, level, owner, onlyPublic, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<CustomerVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(customerService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<CustomerVO> create(@RequestBody CustomerRequest request) {
        try {
            return ApiResult.ok(customerService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<CustomerVO> update(@RequestBody CustomerRequest request) {
        try {
            return ApiResult.ok(customerService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody CustomerDeleteRequest request) {
        try {
            customerService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public ApiResult<Integer> batchDelete(@RequestBody CustomerBatchDeleteRequest request) {
        return ApiResult.ok(customerService.batchDelete(request));
    }

    @PostMapping("/transfer")
    public ApiResult<Integer> transfer(@RequestBody CustomerTransferRequest request) {
        try {
            return ApiResult.ok(customerService.transfer(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/import")
    public ApiResult<CustomerImportResult> importBatch(@RequestBody CustomerImportRequest request) {
        return ApiResult.ok(customerService.importBatch(request));
    }
}
