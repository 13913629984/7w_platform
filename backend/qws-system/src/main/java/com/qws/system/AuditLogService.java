package com.qws.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qws.common.dto.audit.AuditLogCreateRequest;
import com.qws.common.dto.audit.AuditLogListResult;
import com.qws.common.dto.audit.AuditLogVO;
import com.qws.common.entity.SysAuditLog;
import com.qws.common.mapper.SysAuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_QUERY_DAYS = 30;

    private final SysAuditLogMapper sysAuditLogMapper;

    public AuditLogService(SysAuditLogMapper sysAuditLogMapper) {
        this.sysAuditLogMapper = sysAuditLogMapper;
    }

    public AuditLogListResult list(String keyword, String moduleCode, String actionType, String status, String startDate, String endDate, int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<SysAuditLog> query = buildQuery(keyword, moduleCode, actionType, status, startDate, endDate);
        query.orderByDesc(SysAuditLog::getOperateTime).orderByDesc(SysAuditLog::getId);
        Page<SysAuditLog> pageResult = sysAuditLogMapper.selectPage(new Page<>(currentPage, currentPageSize), query);

        AuditLogListResult result = new AuditLogListResult();
        result.setList(pageResult.getRecords().stream().map(this::toVO).toList());
        result.setTotal(pageResult.getTotal());
        result.setPage(currentPage);
        result.setPageSize(currentPageSize);
        result.setStats(stats());
        return result;
    }

    public AuditLogVO create(AuditLogCreateRequest request) {
        SysAuditLog log = new SysAuditLog();
        log.setOperateTime(LocalDateTime.now());
        log.setUsername(defaultText(request.getUsername(), "system"));
        log.setModuleCode(defaultText(request.getModuleCode(), "SYS"));
        log.setModuleName(defaultText(request.getModuleName(), log.getModuleCode()));
        log.setActionType(defaultText(request.getActionType(), "QUERY"));
        log.setActionName(defaultText(request.getActionName(), log.getActionType()));
        log.setContent(defaultText(request.getContent(), "\u8bb0\u5f55\u64cd\u4f5c\u65e5\u5fd7"));
        log.setTargetName(defaultText(request.getTargetName(), "-"));
        log.setIpAddress(defaultText(request.getIpAddress(), "0.0.0.0"));
        log.setStatus(defaultText(request.getStatus(), "success"));
        log.setCostMs(request.getCostMs() == null ? 0 : request.getCostMs());
        log.setRequestId(request.getRequestId());
        log.setBrowser(request.getBrowser());
        log.setDetail(request.getDetail());
        sysAuditLogMapper.insert(log);
        return toVO(log);
    }

    public AuditLogVO detail(Long id) {
        SysAuditLog log = sysAuditLogMapper.selectById(id);
        return log == null ? null : toVO(log);
    }

    private LambdaQueryWrapper<SysAuditLog> buildQuery(String keyword, String moduleCode, String actionType, String status, String startDate, String endDate) {
        LambdaQueryWrapper<SysAuditLog> query = new LambdaQueryWrapper<>();

        // Default time range: last 30 days when no date filter is provided, to avoid full table scan
        LocalDateTime start = parseDateStart(startDate);
        LocalDateTime end = parseDateEnd(endDate);
        if (start == null) {
            start = LocalDate.now().minusDays(DEFAULT_QUERY_DAYS).atStartOfDay();
        }
        query.ge(SysAuditLog::getOperateTime, start);
        if (end != null) {
            query.le(SysAuditLog::getOperateTime, end);
        }

        if (StringUtils.hasText(keyword)) {
            // Use LIKE only on content field; use exact match for username/targetName/ipAddress
            query.and(wrapper -> {
                wrapper.like(SysAuditLog::getContent, keyword)
                        .or().eq(SysAuditLog::getUsername, keyword)
                        .or().eq(SysAuditLog::getTargetName, keyword)
                        .or().eq(SysAuditLog::getIpAddress, keyword);
            });
        }
        if (StringUtils.hasText(moduleCode)) {
            query.eq(SysAuditLog::getModuleCode, moduleCode);
        }
        if (StringUtils.hasText(actionType)) {
            query.eq(SysAuditLog::getActionType, actionType);
        }
        if (StringUtils.hasText(status)) {
            query.eq(SysAuditLog::getStatus, status);
        }
        return query;
    }

    /**
     * Aggregate status counts using a single GROUP BY query instead of 4 separate COUNT queries.
     * Today count uses the operate_time index for efficient lookup.
     */
    private Map<String, Object> stats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // Today count: uses idx_operate_time index
        long todayCount = sysAuditLogMapper.selectCount(
                new LambdaQueryWrapper<SysAuditLog>()
                        .ge(SysAuditLog::getOperateTime, startOfDay)
                        .le(SysAuditLog::getOperateTime, endOfDay));

        // Single GROUP BY query for status counts instead of 3 separate COUNT queries
        QueryWrapper<SysAuditLog> groupQuery = new QueryWrapper<>();
        groupQuery.select("status, COUNT(*) as cnt")
                .groupBy("status");
        List<Map<String, Object>> statusCounts = sysAuditLogMapper.selectMaps(groupQuery);

        long successCount = 0;
        long failCount = 0;
        long pendingCount = 0;
        for (Map<String, Object> row : statusCounts) {
            String st = (String) row.get("status");
            Number cnt = (Number) row.get("cnt");
            if (cnt == null) continue;
            if ("success".equals(st)) {
                successCount = cnt.longValue();
            } else if ("fail".equals(st)) {
                failCount = cnt.longValue();
            } else if ("pending".equals(st)) {
                pendingCount = cnt.longValue();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("today", todayCount);
        result.put("success", successCount);
        result.put("fail", failCount);
        result.put("warn", pendingCount);
        return result;
    }

    private AuditLogVO toVO(SysAuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(log.getId());
        vo.setOperateTime(log.getOperateTime() == null ? "" : log.getOperateTime().format(DATE_TIME_FORMATTER));
        vo.setUsername(log.getUsername());
        vo.setModuleCode(log.getModuleCode());
        vo.setModuleName(log.getModuleName());
        vo.setActionType(log.getActionType());
        vo.setActionName(log.getActionName());
        vo.setContent(log.getContent());
        vo.setTargetName(log.getTargetName());
        vo.setIpAddress(log.getIpAddress());
        vo.setStatus(log.getStatus());
        vo.setCostMs(log.getCostMs());
        vo.setCostText(formatCost(log.getCostMs()));
        vo.setRequestId(log.getRequestId());
        vo.setBrowser(log.getBrowser());
        vo.setDetail(log.getDetail());
        return vo;
    }

    private String formatCost(Integer costMs) {
        if (costMs == null) {
            return "0.00s";
        }
        return String.format("%.2fs", costMs / 1000.0);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private LocalDateTime parseDateStart(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseDateEnd(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value).atTime(LocalTime.MAX);
    }
}
