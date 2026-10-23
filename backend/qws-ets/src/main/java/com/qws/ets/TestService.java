package com.qws.ets;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.ets.TestDeleteRequest;
import com.qws.common.dto.ets.TestListResult;
import com.qws.common.dto.ets.TestRequest;
import com.qws.common.dto.ets.TestResultRequest;
import com.qws.common.dto.ets.TestVO;
import com.qws.common.entity.EtsRegister;
import com.qws.common.entity.EtsTest;
import com.qws.common.mapper.EtsRegisterMapper;
import com.qws.common.mapper.EtsTestMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电测-测试执行服务：负责测试记录的分页查询、明细、增删改、结果录入与统计。
 */
@Service
public class TestService {

    private static final String PREFIX = "T";
    private static final List<String> RESULTS = List.of("pass", "fail");
    private static final List<String> CATEGORIES = List.of(
            "appearance", "insulation", "withstand", "grounding", "temperature", "performance");

    private final EtsTestMapper testMapper;
    private final EtsRegisterMapper registerMapper;

    public TestService(EtsTestMapper testMapper, EtsRegisterMapper registerMapper) {
        this.testMapper = testMapper;
        this.registerMapper = registerMapper;
    }

    public TestListResult list(String keyword, String result, String category, String registerNo,
                               int page, int pageSize) {
        int current = EtsSupport.normalizePage(page);
        int size = EtsSupport.normalizePageSize(pageSize);
        LambdaQueryWrapper<EtsTest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EtsTest::getCode, kw)
                    .or().like(EtsTest::getRegisterNo, kw)
                    .or().like(EtsTest::getCustomerName, kw)
                    .or().like(EtsTest::getProduct, kw)
                    .or().like(EtsTest::getTestItem, kw)
                    .or().like(EtsTest::getTester, kw));
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(EtsTest::getResult, result.trim());
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(EtsTest::getCategory, category.trim());
        }
        if (StringUtils.hasText(registerNo)) {
            wrapper.eq(EtsTest::getRegisterNo, registerNo.trim());
        }
        wrapper.orderByDesc(EtsTest::getTestTime).orderByDesc(EtsTest::getId);

        Page<EtsTest> pageResult = testMapper.selectPage(new Page<>(current, size), wrapper);
        TestListResult payload = new TestListResult();
        payload.setRows(pageResult.getRecords().stream().map(TestVO::from).toList());
        payload.setTotal(pageResult.getTotal());
        payload.setPage(current);
        payload.setPageSize(size);
        payload.setStats(statsInternal());
        return payload;
    }

    public TestVO detail(Long id) {
        return TestVO.from(require(id));
    }

    public TestVO create(TestRequest request) {
        validate(request);
        EtsTest test = new EtsTest();
        applyRequest(test, request);
        if (!StringUtils.hasText(test.getCode())) {
            test.setCode(nextCode());
        }
        if (!StringUtils.hasText(test.getResult())) {
            test.setResult("pending");
        }
        test.setCreateTime(LocalDateTime.now());
        test.setUpdateTime(LocalDateTime.now());
        testMapper.insert(test);
        return TestVO.from(test);
    }

    public TestVO update(TestRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的测试记录");
        }
        validate(request);
        EtsTest test = require(request.getId());
        applyRequest(test, request);
        test.setUpdateTime(LocalDateTime.now());
        testMapper.updateById(test);
        return TestVO.from(test);
    }

    public void delete(TestDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的测试记录");
        }
        require(request.getId());
        testMapper.deleteById(request.getId());
    }

    /** 结果录入：只允许 pass/fail，同时写入测试时间 */
    public TestVO submitResult(TestResultRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要录入结果的测试记录");
        }
        if (!StringUtils.hasText(request.getResult())) {
            throw new IllegalArgumentException("请选择测试结果");
        }
        String result = request.getResult().trim().toLowerCase();
        if (!RESULTS.contains(result)) {
            throw new IllegalArgumentException("测试结果只能为 pass 或 fail");
        }
        EtsTest test = require(request.getId());
        test.setResult(result);
        if (StringUtils.hasText(request.getMeasuredValue())) {
            test.setMeasuredValue(request.getMeasuredValue());
        }
        if (StringUtils.hasText(request.getTester())) {
            test.setTester(request.getTester().trim());
        }
        if (StringUtils.hasText(request.getRemark())) {
            test.setRemark(request.getRemark());
        }
        test.setTestTime(LocalDateTime.now());
        test.setUpdateTime(LocalDateTime.now());
        testMapper.updateById(test);
        return TestVO.from(test);
    }

    public Map<String, Object> stats() {
        return statsInternal();
    }

    /** 来货登记下拉选项（测试关联用） */
    public List<Map<String, Object>> registerOptions() {
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
            option.put("testStandard", r.getTestStandard());
            option.put("status", r.getStatus());
            options.add(option);
        }
        return options;
    }

    // ================= 内部实现 =================

    private EtsTest require(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("测试记录不存在");
        }
        EtsTest test = testMapper.selectById(id);
        if (test == null) {
            throw new IllegalArgumentException("测试记录不存在");
        }
        return test;
    }

    /** 自动生成测试编号：T + yyyyMMdd + 3 位序号 */
    private String nextCode() {
        String prefix = PREFIX + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = testMapper.selectCount(new LambdaQueryWrapper<EtsTest>()
                .likeRight(EtsTest::getCode, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private void validate(TestRequest request) {
        if (!StringUtils.hasText(request.getTestItem())) {
            throw new IllegalArgumentException("请填写测试项目");
        }
        if (StringUtils.hasText(request.getCategory()) && !CATEGORIES.contains(request.getCategory().trim())) {
            throw new IllegalArgumentException("测试类别不正确");
        }
        if (StringUtils.hasText(request.getResult())
                && !RESULTS.contains(request.getResult().trim())
                && !"pending".equals(request.getResult().trim())) {
            throw new IllegalArgumentException("测试结果只能为 pass、fail 或 pending");
        }
        if (request.getRegisterId() == null && !StringUtils.hasText(request.getRegisterNo())) {
            throw new IllegalArgumentException("请选择关联的来货登记");
        }
    }

    private void applyRequest(EtsTest test, TestRequest request) {
        test.setTestItem(request.getTestItem().trim());
        test.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : "appearance");
        test.setTestStandard(request.getTestStandard());
        test.setStandardValue(request.getStandardValue());
        test.setMeasuredValue(request.getMeasuredValue());
        if (StringUtils.hasText(request.getResult())) {
            test.setResult(request.getResult().trim());
        }
        test.setTester(request.getTester());
        test.setTestTime(EtsSupport.parseDateTime(request.getTestTime()));
        test.setDurationMin(EtsSupport.parseDecimal(request.getDurationMin()));
        test.setEquipmentUsed(request.getEquipmentUsed());
        test.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getCode())) {
            test.setCode(request.getCode().trim());
        }
        fillRegister(test, request.getRegisterId(), request.getRegisterNo());
    }

    /**
     * 关联来货登记：优先按 registerId，其次按 registerNo，
     * 自动回填 registerNo / customerName / product / model。
     */
    private void fillRegister(EtsTest test, Long registerId, String registerNo) {
        EtsRegister register = null;
        if (registerId != null) {
            register = registerMapper.selectById(registerId);
            if (register == null) {
                throw new IllegalArgumentException("关联的来货登记不存在");
            }
        } else if (StringUtils.hasText(registerNo)) {
            register = registerMapper.selectOne(new LambdaQueryWrapper<EtsRegister>()
                    .eq(EtsRegister::getRegisterNo, registerNo.trim()).last("LIMIT 1"));
            if (register == null) {
                throw new IllegalArgumentException("关联的来货登记不存在");
            }
        }
        if (register == null) {
            return;
        }
        test.setRegisterId(register.getId());
        test.setRegisterNo(register.getRegisterNo());
        test.setCustomerName(register.getCustomerName());
        test.setProduct(register.getProduct());
        test.setModel(register.getModel());
        if (!StringUtils.hasText(test.getTestStandard())) {
            test.setTestStandard(register.getTestStandard());
        }
    }

    private Map<String, Object> statsInternal() {
        List<EtsTest> all = testMapper.selectList(null);
        int total = all.size();
        int pass = 0;
        int fail = 0;
        int pending = 0;
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        for (String c : CATEGORIES) {
            byCategory.put(c, 0);
        }
        LocalDate today = LocalDate.now();
        long todayCount = 0;
        for (EtsTest t : all) {
            String result = StringUtils.hasText(t.getResult()) ? t.getResult() : "pending";
            switch (result) {
                case "pass" -> pass++;
                case "fail" -> fail++;
                default -> pending++;
            }
            if (StringUtils.hasText(t.getCategory())) {
                byCategory.merge(t.getCategory(), 1, Integer::sum);
            }
            if (t.getTestTime() != null && t.getTestTime().toLocalDate().equals(today)) {
                todayCount++;
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("pass", pass);
        stats.put("fail", fail);
        stats.put("pending", pending);
        stats.put("passRate", EtsSupport.rate(pass, pass + fail));
        stats.put("todayCount", todayCount);
        stats.put("byCategory", byCategory);
        return stats;
    }
}
