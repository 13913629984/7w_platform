package com.qws.crm;

import com.qws.common.ApiResult;
import com.qws.common.dto.contact.ContactBatchDeleteRequest;
import com.qws.common.dto.contact.ContactDeleteRequest;
import com.qws.common.dto.contact.ContactListResult;
import com.qws.common.dto.contact.ContactRequest;
import com.qws.common.dto.contact.ContactVO;
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
@RequestMapping("/crm/contact")
public class ContactController {

    private final CrmContactService contactService;

    public ContactController(CrmContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/list")
    public ApiResult<ContactListResult> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(contactService.list(keyword, customerId, page, pageSize));
    }

    @GetMapping("/customers")
    public ApiResult<List<Map<String, Object>>> customers() {
        return ApiResult.ok(contactService.customerOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<ContactVO> detail(@PathVariable Long id) {
        try {
            return ApiResult.ok(contactService.detail(id));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResult<ContactVO> create(@RequestBody ContactRequest request) {
        try {
            return ApiResult.ok(contactService.create(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public ApiResult<ContactVO> update(@RequestBody ContactRequest request) {
        try {
            return ApiResult.ok(contactService.update(request));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResult<Void> delete(@RequestBody ContactDeleteRequest request) {
        try {
            contactService.delete(request);
            return ApiResult.ok(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public ApiResult<Integer> batchDelete(@RequestBody ContactBatchDeleteRequest request) {
        return ApiResult.ok(contactService.batchDelete(request));
    }
}
