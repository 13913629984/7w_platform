package com.qws.ets;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.ets.RegisterDeleteRequest;
import com.qws.common.dto.ets.RegisterListResult;
import com.qws.common.dto.ets.RegisterRequest;
import com.qws.common.dto.ets.RegisterVO;
import com.qws.common.entity.EtsRegister;
import com.qws.common.mapper.EtsRegisterMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电测-来货登记服务：负责来货登记的分页查询、明细、增删改、状态统计与下拉选项。
 */
@Service
public class RegisterService {

    private static final String PREFIX = "ETS";
    private static final List<String> STATUSES = List.of("pending", "received", "testing", "done", "returned");

    private final EtsRegisterMapper registerMapper;

    public RegisterService(EtsRegisterMapper registerMapper) {
        this.registerMapper = registerMapper;
    }

    public RegisterListResult list(String keyword, String status, String customerName, int page, int pageSize) {
        int current = EtsSupport.normalizePage(page);
        int size = EtsSupport.normalizePageSize(pageSize);
        LambdaQueryWrapper<EtsRegister> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EtsRegister::getRegisterNo, kw)
                    .or().like(EtsRegister::getCustomerName, kw)
                    .or().like(EtsRegister::getProduct, kw)
                    .or().like(EtsRegister::getModel, kw)
                    .or().like(EtsRegister::getBatchNo, kw));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(EtsRegister::getStatus, status.trim());
        }
        if (StringUtils.hasText(customerName)) {
            wrapper.like(EtsRegister::getCustomerName, customerName.trim());
        }
        wrapper.orderByDesc(EtsRegister::getCreateTime).orderByDesc(EtsRegister::getId);

        Page<EtsRegister> result = registerMapper.selectPage(new Page<>(current, size), wrapper);
        RegisterListResult payload = new RegisterListResult();
        payload.setRows(result.getRecords().stream().map(RegisterVO::from).toList());
        payload.setTotal(result.getTotal());
        payload.setPage(current);
        payload.setPageSize(size);
        payload.setStats(statsInternal());
        return payload;
    }

    public RegisterVO detail(Long id) {
        return RegisterVO.from(require(id));
    }

    public RegisterVO create(RegisterRequest request) {
        validate(request);
        EtsRegister register = new EtsRegister();
        applyRequest(register, request);
        if (!StringUtils.hasText(register.getRegisterNo())) {
            register.setRegisterNo(nextRegisterNo());
        }
        if (!StringUtils.hasText(register.getStatus())) {
            register.setStatus("pending");
        }
        if (register.getArriveDate() == null) {
            register.setArriveDate(LocalDate.now());
        }
        register.setCreateTime(LocalDateTime.now());
        register.setUpdateTime(LocalDateTime.now());
        registerMapper.insert(register);
        return RegisterVO.from(register);
    }

    public RegisterVO update(RegisterRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的来货登记");
        }
        validate(request);
        EtsRegister register = require(request.getId());
        applyRequest(register, request);
        if (StringUtils.hasText(register.getStatus()) && !STATUSES.contains(register.getStatus())) {
            throw new IllegalArgumentException("来货状态不正确");
        }
        register.setUpdateTime(LocalDateTime.now());
        registerMapper.updateById(register);
        return RegisterVO.from(register);
    }

    public void delete(RegisterDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的来货登记");
        }
        require(request.getId());
        registerMapper.deleteById(request.getId());
    }

    public Map<String, Object> stats() {
        return statsInternal();
    }

    /** 来货登记下拉选项：登记编号 + 产品 */
    public List<Map<String, Object>> options() {
        List<EtsRegister> all = registerMapper.selectList(new LambdaQueryWrapper<EtsRegister>()
                .orderByDesc(EtsRegister::getCreateTime).orderByDesc(EtsRegister::getId));
        List<Map<String, Object>> options = new ArrayList<>();
        for (EtsRegister r : all) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", r.getId());
            option.put("registerNo", r.getRegisterNo());
            option.put("customerName", r.getCustomerName());
            option.put("product", r.getProduct());
            option.put("model", r.getModel());
            option.put("batchNo", r.getBatchNo());
            option.put("quantity", r.getQuantity());
            option.put("sampleQty", r.getSampleQty());
            option.put("status", r.getStatus());
            options.add(option);
        }
        return options;
    }

    // ================= 内部实现 =================

    private EtsRegister require(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("来货登记不存在");
        }
        EtsRegister register = registerMapper.selectById(id);
        if (register == null) {
            throw new IllegalArgumentException("来货登记不存在");
        }
        return register;
    }

    /** 自动生成登记编号：ETS + yyyyMMdd + 3 位序号 */
    private String nextRegisterNo() {
        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = PREFIX + today;
        Long count = registerMapper.selectCount(new LambdaQueryWrapper<EtsRegister>()
                .likeRight(EtsRegister::getRegisterNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private void validate(RegisterRequest request) {
        if (!StringUtils.hasText(request.getCustomerName())) {
            throw new IllegalArgumentException("请填写客户名称");
        }
        if (!StringUtils.hasText(request.getProduct())) {
            throw new IllegalArgumentException("请填写产品名称");
        }
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new IllegalArgumentException("来货数量不能为负数");
        }
        if (request.getSampleQty() != null && request.getSampleQty() < 0) {
            throw new IllegalArgumentException("抽样数量不能为负数");
        }
    }

    private void applyRequest(EtsRegister register, RegisterRequest request) {
        register.setCustomerName(request.getCustomerName().trim());
        register.setContact(request.getContact());
        register.setPhone(request.getPhone());
        register.setProduct(request.getProduct().trim());
        register.setModel(request.getModel());
        register.setBrand(request.getBrand());
        register.setBatchNo(request.getBatchNo());
        register.setQuantity(request.getQuantity());
        register.setSampleQty(request.getSampleQty());
        if (StringUtils.hasText(request.getStatus())) {
            register.setStatus(request.getStatus().trim());
        }
        register.setArriveDate(EtsSupport.parseDate(request.getArriveDate()));
        register.setExpectDate(EtsSupport.parseDate(request.getExpectDate()));
        register.setTestStandard(request.getTestStandard());
        register.setOperator(request.getOperator());
        register.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getRegisterNo())) {
            register.setRegisterNo(request.getRegisterNo().trim());
        }
    }

    private Map<String, Object> statsInternal() {
        List<EtsRegister> all = registerMapper.selectList(null);
        Map<String, Integer> byStatus = new HashMap<>();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        long monthAdded = 0;
        int totalQuantity = 0;
        for (EtsRegister r : all) {
            String status = StringUtils.hasText(r.getStatus()) ? r.getStatus() : "pending";
            byStatus.merge(status, 1, Integer::sum);
            if (r.getCreateTime() != null && !r.getCreateTime().toLocalDate().isBefore(firstDayOfMonth)) {
                monthAdded++;
            }
            if (r.getQuantity() != null) {
                totalQuantity += r.getQuantity();
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("pending", byStatus.getOrDefault("pending", 0));
        stats.put("received", byStatus.getOrDefault("received", 0));
        stats.put("testing", byStatus.getOrDefault("testing", 0));
        stats.put("done", byStatus.getOrDefault("done", 0));
        stats.put("returned", byStatus.getOrDefault("returned", 0));
        stats.put("monthAdded", monthAdded);
        stats.put("totalQuantity", totalQuantity);
        return stats;
    }
}
