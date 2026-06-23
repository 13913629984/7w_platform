package com.qws.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.customer.CustomerBatchDeleteRequest;
import com.qws.common.dto.customer.CustomerContactDTO;
import com.qws.common.dto.customer.CustomerDeleteRequest;
import com.qws.common.dto.customer.CustomerImportRequest;
import com.qws.common.dto.customer.CustomerImportResult;
import com.qws.common.dto.customer.CustomerListResult;
import com.qws.common.dto.customer.CustomerRequest;
import com.qws.common.dto.customer.CustomerSalesDTO;
import com.qws.common.dto.customer.CustomerTransferRequest;
import com.qws.common.dto.customer.CustomerVO;
import com.qws.common.entity.CrmCustomer;
import com.qws.common.entity.CrmCustomerContact;
import com.qws.common.entity.CrmCustomerSales;
import com.qws.common.mapper.CrmCustomerContactMapper;
import com.qws.common.mapper.CrmCustomerMapper;
import com.qws.common.mapper.CrmCustomerSalesMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CrmCustomerService {

    private static final List<String> LEVELS = List.of("A级", "B级", "C级", "D级");

    private final CrmCustomerMapper customerMapper;
    private final CrmCustomerSalesMapper salesMapper;
    private final CrmCustomerContactMapper contactMapper;

    public CrmCustomerService(CrmCustomerMapper customerMapper,
                              CrmCustomerSalesMapper salesMapper,
                              CrmCustomerContactMapper contactMapper) {
        this.customerMapper = customerMapper;
        this.salesMapper = salesMapper;
        this.contactMapper = contactMapper;
    }

    public CustomerListResult list(String name, String level, String owner, boolean onlyPublic, int page, int pageSize) {
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(CrmCustomer::getName, name);
        }
        if (StringUtils.hasText(level)) {
            wrapper.eq(CrmCustomer::getLevel, level);
        }
        if (StringUtils.hasText(owner)) {
            wrapper.like(CrmCustomer::getOwner, owner);
        }
        if (onlyPublic) {
            // 公海客户：未分配销售负责人（owner 为空）
            wrapper.and(w -> w.isNull(CrmCustomer::getOwner).or().eq(CrmCustomer::getOwner, ""));
        }
        wrapper.orderByDesc(CrmCustomer::getUpdateTime);
        Page<CrmCustomer> result = customerMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<CustomerVO> vos = result.getRecords().stream().map(this::toVO).toList();
        CustomerListResult payload = new CustomerListResult();
        payload.setRows(vos);
        payload.setTotal(result.getTotal());
        payload.setPage(page);
        payload.setPageSize(pageSize);
        payload.setStats(computeStats());
        payload.setLevels(LEVELS);
        return payload;
    }

    public CustomerVO detail(Long id) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        return toVO(customer);
    }

    @Transactional
    public CustomerVO create(CustomerRequest request) {
        validate(request);
        CrmCustomer customer = new CrmCustomer();
        applyRequest(customer, request);
        if (customer.getStatus() == null) {
            customer.setStatus(1);
        }
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.insert(customer);
        saveChildren(customer.getId(), request);
        return toVO(customer);
    }

    @Transactional
    public CustomerVO update(CustomerRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的客户");
        }
        validate(request);
        CrmCustomer customer = customerMapper.selectById(request.getId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        applyRequest(customer, request);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);
        deleteChildren(customer.getId());
        saveChildren(customer.getId(), request);
        return toVO(customer);
    }

    @Transactional
    public void delete(CustomerDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的客户");
        }
        CrmCustomer customer = customerMapper.selectById(request.getId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        customerMapper.deleteById(request.getId());
        deleteChildren(request.getId());
    }

    @Transactional
    public int batchDelete(CustomerBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }
        for (Long id : request.getIds()) {
            deleteChildren(id);
        }
        return customerMapper.deleteByIds(request.getIds());
    }

    @Transactional
    public int transfer(CustomerTransferRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("请选择要转移的客户");
        }
        if (!StringUtils.hasText(request.getOwner())) {
            throw new IllegalArgumentException("请输入新的销售负责人");
        }
        String owner = request.getOwner().trim();
        int count = 0;
        for (Long id : request.getIds()) {
            CrmCustomer customer = customerMapper.selectById(id);
            if (customer == null) {
                continue;
            }
            customer.setOwner(owner);
            customer.setUpdateTime(LocalDateTime.now());
            customerMapper.updateById(customer);
            // 同步销售列表的负责人标记
            List<CrmCustomerSales> salesList = salesMapper.selectList(new LambdaQueryWrapper<CrmCustomerSales>()
                    .eq(CrmCustomerSales::getCustomerId, id));
            boolean matched = false;
            for (CrmCustomerSales sales : salesList) {
                boolean isOwner = owner.equals(sales.getName());
                sales.setIsOwner(isOwner ? 1 : 0);
                salesMapper.updateById(sales);
                if (isOwner) {
                    matched = true;
                }
            }
            if (!matched) {
                CrmCustomerSales sales = new CrmCustomerSales();
                sales.setCustomerId(id);
                sales.setName(owner);
                sales.setPosition("");
                sales.setIsOwner(1);
                salesMapper.insert(sales);
            }
            count++;
        }
        return count;
    }

    @Transactional
    public CustomerImportResult importBatch(CustomerImportRequest request) {
        CustomerImportResult result = new CustomerImportResult();
        List<CustomerVO> created = new ArrayList<>();
        List<String> failedNames = new ArrayList<>();
        int failed = 0;
        if (request.getItems() != null) {
            for (CustomerRequest item : request.getItems()) {
                try {
                    created.add(create(item));
                } catch (Exception ex) {
                    failed++;
                    failedNames.add((item.getName() == null ? "(空)" : item.getName()) + "(" + ex.getMessage() + ")");
                }
            }
        }
        result.setItems(created);
        result.setSuccessCount(created.size());
        result.setFailedCount(failed);
        result.setFailedNames(failedNames);
        return result;
    }

    private void validate(CustomerRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写客户名称");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("客户名称长度不能超过100个字符");
        }
        if (!StringUtils.hasText(request.getLevel())) {
            throw new IllegalArgumentException("请选择客户等级");
        }
        if (!LEVELS.contains(request.getLevel())) {
            throw new IllegalArgumentException("客户等级不合法");
        }
    }

    private void applyRequest(CrmCustomer customer, CustomerRequest request) {
        customer.setName(request.getName());
        customer.setEnglishName(request.getEnglishName());
        customer.setAddress(request.getAddress());
        customer.setLevel(request.getLevel());
        customer.setOwner(resolveOwner(request));
        customer.setLastVisitAt(parseDate(request.getLastVisitAt()));
        customer.setLastDealAt(parseDate(request.getLastDealAt()));
        customer.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }
    }

    private String resolveOwner(CustomerRequest request) {
        if (request.getSalesList() != null) {
            CustomerSalesDTO owner = request.getSalesList().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsOwner()) && StringUtils.hasText(s.getName()))
                    .findFirst()
                    .orElseGet(() -> request.getSalesList().stream()
                            .filter(s -> StringUtils.hasText(s.getName()))
                            .findFirst()
                            .orElse(null));
            if (owner != null) {
                return owner.getName().trim();
            }
        }
        return StringUtils.hasText(request.getOwner()) ? request.getOwner().trim() : "";
    }

    private void saveChildren(Long customerId, CustomerRequest request) {
        if (request.getSalesList() != null) {
            for (CustomerSalesDTO dto : request.getSalesList()) {
                if (!StringUtils.hasText(dto.getName())) {
                    continue;
                }
                CrmCustomerSales sales = new CrmCustomerSales();
                sales.setCustomerId(customerId);
                sales.setName(dto.getName().trim());
                sales.setPosition(dto.getPosition());
                sales.setIsOwner(Boolean.TRUE.equals(dto.getIsOwner()) ? 1 : 0);
                salesMapper.insert(sales);
            }
        }
        if (request.getContactList() != null) {
            for (CustomerContactDTO dto : request.getContactList()) {
                if (!StringUtils.hasText(dto.getName()) && !StringUtils.hasText(dto.getPhone()) && !StringUtils.hasText(dto.getTitle())) {
                    continue;
                }
                CrmCustomerContact contact = new CrmCustomerContact();
                contact.setCustomerId(customerId);
                contact.setName(dto.getName());
                contact.setPhone(dto.getPhone());
                contact.setTitle(dto.getTitle());
                contactMapper.insert(contact);
            }
        }
    }

    private void deleteChildren(Long customerId) {
        salesMapper.delete(new LambdaQueryWrapper<CrmCustomerSales>().eq(CrmCustomerSales::getCustomerId, customerId));
        contactMapper.delete(new LambdaQueryWrapper<CrmCustomerContact>().eq(CrmCustomerContact::getCustomerId, customerId));
    }

    private CustomerVO toVO(CrmCustomer customer) {
        List<CustomerSalesDTO> salesList = salesMapper.selectList(new LambdaQueryWrapper<CrmCustomerSales>()
                        .eq(CrmCustomerSales::getCustomerId, customer.getId())
                        .orderByAsc(CrmCustomerSales::getId)).stream()
                .map(s -> {
                    CustomerSalesDTO dto = new CustomerSalesDTO();
                    dto.setId(s.getId());
                    dto.setName(s.getName());
                    dto.setPosition(s.getPosition());
                    dto.setIsOwner(s.getIsOwner() != null && s.getIsOwner() == 1);
                    return dto;
                }).collect(Collectors.toList());
        List<CustomerContactDTO> contactList = contactMapper.selectList(new LambdaQueryWrapper<CrmCustomerContact>()
                        .eq(CrmCustomerContact::getCustomerId, customer.getId())
                        .orderByAsc(CrmCustomerContact::getId)).stream()
                .map(c -> {
                    CustomerContactDTO dto = new CustomerContactDTO();
                    dto.setId(c.getId());
                    dto.setName(c.getName());
                    dto.setPhone(c.getPhone());
                    dto.setTitle(c.getTitle());
                    return dto;
                }).collect(Collectors.toList());
        return CustomerVO.from(customer, salesList, contactList);
    }

    private Map<String, Object> computeStats() {
        List<CrmCustomer> all = customerMapper.selectList(null);
        Map<String, Object> stats = new HashMap<>();
        int total = all.size();
        long active = all.stream().filter(c -> "A级".equals(c.getLevel()) || "B级".equals(c.getLevel())).count();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        long monthAdded = all.stream()
                .filter(c -> c.getCreateTime() != null && !c.getCreateTime().toLocalDate().isBefore(firstDayOfMonth))
                .count();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("monthAdded", monthAdded);
        stats.put("conversionRate", total == 0 ? 0 : Math.round((double) active / total * 100));
        Map<String, Long> byLevel = all.stream()
                .filter(c -> c.getLevel() != null)
                .collect(Collectors.groupingBy(CrmCustomer::getLevel, Collectors.counting()));
        stats.put("byLevel", byLevel.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()), (a, b) -> a, java.util.LinkedHashMap::new)));
        return stats;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ex) {
            return null;
        }
    }
}
