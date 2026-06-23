package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.contact.ContactBatchDeleteRequest;
import com.qws.common.dto.contact.ContactDeleteRequest;
import com.qws.common.dto.contact.ContactListResult;
import com.qws.common.dto.contact.ContactRequest;
import com.qws.common.dto.contact.ContactVO;
import com.qws.common.entity.CrmContact;
import com.qws.common.entity.CrmCustomer;
import com.qws.common.mapper.CrmContactMapper;
import com.qws.common.mapper.CrmCustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CrmContactService {

    private final CrmContactMapper contactMapper;
    private final CrmCustomerMapper customerMapper;

    public CrmContactService(CrmContactMapper contactMapper, CrmCustomerMapper customerMapper) {
        this.contactMapper = contactMapper;
        this.customerMapper = customerMapper;
    }

    public ContactListResult list(String keyword, Long customerId, int page, int pageSize) {
        LambdaQueryWrapper<CrmContact> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmContact::getName, keyword)
                    .or().like(CrmContact::getPhone, keyword)
                    .or().like(CrmContact::getEmail, keyword));
        }
        if (customerId != null) {
            wrapper.eq(CrmContact::getCustomerId, customerId);
        }
        wrapper.orderByDesc(CrmContact::getCreateTime).orderByDesc(CrmContact::getId);
        Page<CrmContact> result = contactMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<ContactVO> vos = result.getRecords().stream().map(ContactVO::from).toList();
        ContactListResult payload = new ContactListResult();
        payload.setRows(vos);
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        return payload;
    }

    public ContactVO detail(Long id) {
        CrmContact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw new IllegalArgumentException("联系人不存在");
        }
        return ContactVO.from(contact);
    }

    public ContactVO create(ContactRequest request) {
        validate(request);
        CrmContact contact = new CrmContact();
        applyRequest(contact, request);
        contact.setCreateTime(LocalDateTime.now());
        contact.setUpdateTime(LocalDateTime.now());
        contactMapper.insert(contact);
        return ContactVO.from(contact);
    }

    public ContactVO update(ContactRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的联系人");
        }
        validate(request);
        CrmContact contact = contactMapper.selectById(request.getId());
        if (contact == null) {
            throw new IllegalArgumentException("联系人不存在");
        }
        applyRequest(contact, request);
        contact.setUpdateTime(LocalDateTime.now());
        contactMapper.updateById(contact);
        return ContactVO.from(contact);
    }

    public void delete(ContactDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的联系人");
        }
        CrmContact contact = contactMapper.selectById(request.getId());
        if (contact == null) {
            throw new IllegalArgumentException("联系人不存在");
        }
        contactMapper.deleteById(request.getId());
    }

    public int batchDelete(ContactBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }
        return contactMapper.deleteByIds(request.getIds());
    }

    private void validate(ContactRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写联系人姓名");
        }
        if (request.getName().length() > 50) {
            throw new IllegalArgumentException("联系人姓名长度不能超过50个字符");
        }
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().contains("@")) {
            throw new IllegalArgumentException("电子邮箱格式不正确");
        }
    }

    private void applyRequest(CrmContact contact, ContactRequest request) {
        contact.setName(request.getName().trim());
        contact.setTitle(request.getTitle());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setRemark(request.getRemark());
        contact.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()) ? 1 : 0);
        // 解析关联客户：优先按 customerId，其次按名称匹配
        Long customerId = request.getCustomerId();
        String customerName = request.getCustomerName();
        if (customerId != null) {
            CrmCustomer customer = customerMapper.selectById(customerId);
            if (customer != null) {
                customerName = customer.getName();
            }
        } else if (StringUtils.hasText(customerName)) {
            CrmCustomer customer = customerMapper.selectOne(new LambdaQueryWrapper<CrmCustomer>()
                    .eq(CrmCustomer::getName, customerName.trim()).last("LIMIT 1"));
            if (customer != null) {
                customerId = customer.getId();
            }
        }
        contact.setCustomerId(customerId);
        contact.setCustomerName(StringUtils.hasText(customerName) ? customerName.trim() : null);
    }

    private Map<String, Object> computeStats() {
        List<CrmContact> all = contactMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        int total = all.size();
        long primary = all.stream().filter(c -> c.getIsPrimary() != null && c.getIsPrimary() == 1).count();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        long monthAdded = all.stream()
                .filter(c -> c.getCreateTime() != null && !c.getCreateTime().toLocalDate().isBefore(firstDayOfMonth))
                .count();
        long customerCount = all.stream()
                .map(CrmContact::getCustomerName)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
        stats.put("total", total);
        stats.put("primary", primary);
        stats.put("monthAdded", monthAdded);
        stats.put("customerCount", customerCount);
        return stats;
    }

    public List<Map<String, Object>> customerOptions() {
        return customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
                        .orderByDesc(CrmCustomer::getUpdateTime)).stream()
                .map(c -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", c.getId());
                    option.put("name", c.getName());
                    return option;
                }).collect(Collectors.toList());
    }
}
