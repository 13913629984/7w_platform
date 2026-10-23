package com.qws.ets;

import com.qws.common.dto.ets.EtsOverviewVO;
import com.qws.common.entity.EtsRegister;
import com.qws.common.entity.EtsReport;
import com.qws.common.entity.EtsTest;
import com.qws.common.mapper.EtsRegisterMapper;
import com.qws.common.mapper.EtsReportMapper;
import com.qws.common.mapper.EtsTestMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电测-工作台服务：汇总来货登记、测试执行与报告数据，提供总览、趋势与分布视图。
 */
@Service
public class EtsWorkspaceService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> CATEGORIES = List.of(
            "appearance", "insulation", "withstand", "grounding", "temperature", "performance");

    private final EtsRegisterMapper registerMapper;
    private final EtsTestMapper testMapper;
    private final EtsReportMapper reportMapper;

    public EtsWorkspaceService(EtsRegisterMapper registerMapper, EtsTestMapper testMapper,
                               EtsReportMapper reportMapper) {
        this.registerMapper = registerMapper;
        this.testMapper = testMapper;
        this.reportMapper = reportMapper;
    }

    public EtsOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        List<EtsRegister> registers = registerMapper.selectList(null);
        long monthRegister = 0;
        long pendingTest = 0;
        for (EtsRegister r : registers) {
            if (r.getCreateTime() != null && !r.getCreateTime().toLocalDate().isBefore(firstDayOfMonth)) {
                monthRegister++;
            }
            String status = StringUtils.hasText(r.getStatus()) ? r.getStatus() : "pending";
            if ("pending".equals(status) || "received".equals(status) || "testing".equals(status)) {
                pendingTest++;
            }
        }

        List<EtsTest> tests = testMapper.selectList(null);
        long todayTested = 0;
        long pass = 0;
        long fail = 0;
        for (EtsTest t : tests) {
            String result = StringUtils.hasText(t.getResult()) ? t.getResult() : "pending";
            if ("pass".equals(result)) {
                pass++;
            } else if ("fail".equals(result)) {
                fail++;
            }
            if (t.getTestTime() != null && t.getTestTime().toLocalDate().equals(today)) {
                todayTested++;
            }
        }

        List<EtsReport> reports = reportMapper.selectList(null);
        long pendingReport = reports.stream()
                .filter(r -> "pending".equals(r.getStatus()) || "draft".equals(r.getStatus()))
                .count();

        EtsOverviewVO overview = new EtsOverviewVO();
        overview.setMonthRegister(monthRegister);
        overview.setPendingTest(pendingTest);
        overview.setTodayTested(todayTested);
        overview.setPassRate(EtsSupport.rate((int) pass, (int) (pass + fail)));
        overview.setFailCount(fail);
        overview.setPendingReport(pendingReport);
        overview.setTestTrend(trend());
        overview.setResultDistribution(distribution());
        overview.setCategoryDistribution(categoryDistribution(tests));
        return overview;
    }

    /** 近 7 日测试数趋势（按测试时间统计） */
    public List<Map<String, Object>> trend() {
        List<EtsTest> tests = testMapper.selectList(null);
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long count = tests.stream()
                    .filter(t -> t.getTestTime() != null && t.getTestTime().toLocalDate().equals(day))
                    .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(DATE));
            item.put("label", day.getMonthValue() + "/" + day.getDayOfMonth());
            item.put("count", count);
            trend.add(item);
        }
        return trend;
    }

    /** 测试结果分布（pass/fail/pending） */
    public List<Map<String, Object>> distribution() {
        List<EtsTest> tests = testMapper.selectList(null);
        long pass = tests.stream().filter(t -> "pass".equals(t.getResult())).count();
        long fail = tests.stream().filter(t -> "fail".equals(t.getResult())).count();
        long pending = tests.size() - pass - fail;
        List<Map<String, Object>> distribution = new ArrayList<>();
        distribution.add(item("pass", pass));
        distribution.add(item("fail", fail));
        distribution.add(item("pending", pending));
        return distribution;
    }

    /** 测试类别分布 */
    private List<Map<String, Object>> categoryDistribution(List<EtsTest> tests) {
        Map<String, Long> counter = new LinkedHashMap<>();
        for (String category : CATEGORIES) {
            counter.put(category, 0L);
        }
        for (EtsTest t : tests) {
            if (StringUtils.hasText(t.getCategory())) {
                counter.merge(t.getCategory(), 1L, Long::sum);
            }
        }
        List<Map<String, Object>> distribution = new ArrayList<>();
        counter.forEach((name, value) -> distribution.add(item(name, value)));
        return distribution;
    }

    private Map<String, Object> item(String name, long value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }
}
