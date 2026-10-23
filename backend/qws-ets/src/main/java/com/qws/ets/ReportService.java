package com.qws.ets;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.ets.ReportApproveRequest;
import com.qws.common.dto.ets.ReportDeleteRequest;
import com.qws.common.dto.ets.ReportListResult;
import com.qws.common.dto.ets.ReportRequest;
import com.qws.common.dto.ets.ReportVO;
import com.qws.common.entity.EtsRegister;
import com.qws.common.entity.EtsReport;
import com.qws.common.mapper.EtsRegisterMapper;
import com.qws.common.mapper.EtsReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电测-测试报告服务：负责报告的分页查询、明细、增删改、审批签发与统计。
 */
@Service
public class ReportService {

    private static final String PREFIX = "RPT";
    private static final List<String> STATUSES = List.of("draft", "pending", "approved", "rejected");

    private final EtsReportMapper reportMapper;
    private final EtsRegisterMapper registerMapper;

    public ReportService(EtsReportMapper reportMapper, EtsRegisterMapper registerMapper) {
        this.reportMapper = reportMapper;
        this.registerMapper = registerMapper;
    }

    public ReportListResult list(String keyword, String status, String customerName, int page, int pageSize) {
        int current = EtsSupport.normalizePage(page);
        int size = EtsSupport.normalizePageSize(pageSize);
        LambdaQueryWrapper<EtsReport> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EtsReport::getReportNo, kw)
                    .or().like(EtsReport::getRegisterNo, kw)
                    .or().like(EtsReport::getCustomerName, kw)
                    .or().like(EtsReport::getProduct, kw)
                    .or().like(EtsReport::getInspector, kw));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(EtsReport::getStatus, status.trim());
        }
        if (StringUtils.hasText(customerName)) {
            wrapper.like(EtsReport::getCustomerName, customerName.trim());
        }
        wrapper.orderByDesc(EtsReport::getCreateTime).orderByDesc(EtsReport::getId);

        Page<EtsReport> pageResult = reportMapper.selectPage(new Page<>(current, size), wrapper);
        ReportListResult payload = new ReportListResult();
        payload.setRows(pageResult.getRecords().stream().map(ReportVO::from).toList());
        payload.setTotal(pageResult.getTotal());
        payload.setPage(current);
        payload.setPageSize(size);
        payload.setStats(statsInternal());
        return payload;
    }

    public ReportVO detail(Long id) {
        return ReportVO.from(require(id));
    }

    public ReportVO create(ReportRequest request) {
        validate(request);
        EtsReport report = new EtsReport();
        applyRequest(report, request);
        if (!StringUtils.hasText(report.getReportNo())) {
            report.setReportNo(nextReportNo());
        }
        if (!StringUtils.hasText(report.getStatus())) {
            report.setStatus("draft");
        }
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.insert(report);
        return ReportVO.from(report);
    }

    public ReportVO update(ReportRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要修改的测试报告");
        }
        validate(request);
        EtsReport report = require(request.getId());
        applyRequest(report, request);
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);
        return ReportVO.from(report);
    }

    public void delete(ReportDeleteRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要删除的测试报告");
        }
        require(request.getId());
        reportMapper.deleteById(request.getId());
    }

    /** 审批：action=approve 置 approved，action=reject 置 rejected，写审批人与审批时间 */
    public ReportVO approve(ReportApproveRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("请选择要审批的测试报告");
        }
        if (!StringUtils.hasText(request.getAction())) {
            throw new IllegalArgumentException("请选择审批动作");
        }
        String action = request.getAction().trim().toLowerCase();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new IllegalArgumentException("审批动作只能为 approve 或 reject");
        }
        EtsReport report = require(request.getId());
        if ("approved".equals(report.getStatus())) {
            throw new IllegalStateException("报告已审批通过，不能重复审批");
        }
        report.setStatus("approve".equals(action) ? "approved" : "rejected");
        report.setApprover(StringUtils.hasText(request.getApprover()) ? request.getApprover().trim() : "系统管理员");
        report.setApproveTime(LocalDateTime.now());
        if (StringUtils.hasText(request.getRemark())) {
            report.setRemark(request.getRemark());
        }
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);
        return ReportVO.from(report);
    }

    /** 签发：生成报告编号并置为 approved，同时写入签发时间 */
    public ReportVO issue(Long id) {
        EtsReport report = require(id);
        if ("rejected".equals(report.getStatus())) {
            throw new IllegalStateException("报告已被驳回，请先修改后再签发");
        }
        if (!StringUtils.hasText(report.getReportNo())) {
            report.setReportNo(nextReportNo());
        }
        report.setStatus("approved");
        report.setIssueDate(LocalDateTime.now());
        if (!StringUtils.hasText(report.getApprover())) {
            report.setApprover("系统管理员");
        }
        if (report.getApproveTime() == null) {
            report.setApproveTime(LocalDateTime.now());
        }
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);
        return ReportVO.from(report);
    }

    public Map<String, Object> stats() {
        return statsInternal();
    }

    /** 来货登记下拉选项（报告关联用） */
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
            option.put("quantity", r.getQuantity());
            option.put("sampleQty", r.getSampleQty());
            option.put("status", r.getStatus());
            options.add(option);
        }
        return options;
    }

    // ================= 内部实现 =================

    private EtsReport require(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("测试报告不存在");
        }
        EtsReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("测试报告不存在");
        }
        return report;
    }

    /** 自动生成报告编号：RPT + yyyyMMdd + 3 位序号 */
    private String nextReportNo() {
        String prefix = PREFIX + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = reportMapper.selectCount(new LambdaQueryWrapper<EtsReport>()
                .likeRight(EtsReport::getReportNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private void validate(ReportRequest request) {
        if (!StringUtils.hasText(request.getCustomerName())) {
            throw new IllegalArgumentException("请填写客户名称");
        }
        if (!StringUtils.hasText(request.getProduct())) {
            throw new IllegalArgumentException("请填写产品名称");
        }
        if (StringUtils.hasText(request.getStatus()) && !STATUSES.contains(request.getStatus().trim())) {
            throw new IllegalArgumentException("报告状态不正确");
        }
        if (request.getPassQty() != null && request.getPassQty() < 0) {
            throw new IllegalArgumentException("合格数量不能为负数");
        }
        if (request.getFailQty() != null && request.getFailQty() < 0) {
            throw new IllegalArgumentException("不合格数量不能为负数");
        }
    }

    private void applyRequest(EtsReport report, ReportRequest request) {
        report.setCustomerName(request.getCustomerName().trim());
        report.setProduct(request.getProduct().trim());
        report.setModel(request.getModel());
        report.setBatchNo(request.getBatchNo());
        report.setQuantity(request.getQuantity());
        report.setSampleQty(request.getSampleQty());
        report.setPassQty(request.getPassQty());
        report.setFailQty(request.getFailQty());
        report.setPassRate(computePassRate(request.getPassQty(), request.getSampleQty()));
        if (StringUtils.hasText(request.getStatus())) {
            report.setStatus(request.getStatus().trim());
        }
        report.setConclusion(request.getConclusion());
        report.setInspector(request.getInspector());
        report.setIssueDate(EtsSupport.parseDateTime(request.getIssueDate()));
        report.setApprover(request.getApprover());
        report.setApproveTime(EtsSupport.parseDateTime(request.getApproveTime()));
        report.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getReportNo())) {
            report.setReportNo(request.getReportNo().trim());
        }
        fillRegister(report, request);
    }

    /**
     * 关联来货登记：优先按 registerId，其次按 registerNo，
     * 自动回填 registerNo / customerName / product / model / batchNo 等。
     */
    private void fillRegister(EtsReport report, ReportRequest request) {
        EtsRegister register = null;
        if (request.getRegisterId() != null) {
            register = registerMapper.selectById(request.getRegisterId());
            if (register == null) {
                throw new IllegalArgumentException("关联的来货登记不存在");
            }
        } else if (StringUtils.hasText(request.getRegisterNo())) {
            register = registerMapper.selectOne(new LambdaQueryWrapper<EtsRegister>()
                    .eq(EtsRegister::getRegisterNo, request.getRegisterNo().trim()).last("LIMIT 1"));
            if (register == null) {
                throw new IllegalArgumentException("关联的来货登记不存在");
            }
        }
        if (register == null) {
            return;
        }
        report.setRegisterId(register.getId());
        report.setRegisterNo(register.getRegisterNo());
        if (!StringUtils.hasText(request.getCustomerName())) {
            report.setCustomerName(register.getCustomerName());
        }
        if (!StringUtils.hasText(request.getProduct())) {
            report.setProduct(register.getProduct());
        }
        if (!StringUtils.hasText(request.getModel())) {
            report.setModel(register.getModel());
        }
        if (!StringUtils.hasText(request.getBatchNo())) {
            report.setBatchNo(register.getBatchNo());
        }
        if (report.getQuantity() == null) {
            report.setQuantity(register.getQuantity());
        }
        if (report.getSampleQty() == null) {
            report.setSampleQty(register.getSampleQty());
        }
        report.setPassRate(computePassRate(report.getPassQty(), report.getSampleQty()));
    }

    /** passRate = passQty / sampleQty * 100，保留 2 位，sampleQty 为 0 时置 0 */
    private BigDecimal computePassRate(Integer passQty, Integer sampleQty) {
        int pass = passQty == null ? 0 : passQty;
        int sample = sampleQty == null ? 0 : sampleQty;
        return EtsSupport.rate(pass, sample);
    }

    private Map<String, Object> statsInternal() {
        List<EtsReport> all = reportMapper.selectList(null);
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (String s : STATUSES) {
            byStatus.put(s, 0);
        }
        int totalSample = 0;
        int totalPass = 0;
        int totalQuantity = 0;
        for (EtsReport r : all) {
            String status = StringUtils.hasText(r.getStatus()) ? r.getStatus() : "draft";
            byStatus.merge(status, 1, Integer::sum);
            totalSample += r.getSampleQty() == null ? 0 : r.getSampleQty();
            totalPass += r.getPassQty() == null ? 0 : r.getPassQty();
            totalQuantity += r.getQuantity() == null ? 0 : r.getQuantity();
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("draft", byStatus.getOrDefault("draft", 0));
        stats.put("pending", byStatus.getOrDefault("pending", 0));
        stats.put("approved", byStatus.getOrDefault("approved", 0));
        stats.put("rejected", byStatus.getOrDefault("rejected", 0));
        stats.put("totalSample", totalSample);
        stats.put("totalPass", totalPass);
        stats.put("totalQuantity", totalQuantity);
        stats.put("passRate", EtsSupport.rate(totalPass, totalSample));
        return stats;
    }
}
