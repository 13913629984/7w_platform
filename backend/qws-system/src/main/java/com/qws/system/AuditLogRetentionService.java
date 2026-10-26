package com.qws.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.SysAuditLog;
import com.qws.common.mapper.SysAuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志生命周期管理。
 *
 * <p>问题：第 3 批把审计做成"每个写操作都记一条"之后，{@code sys_audit_log} 会持续增长。
 * 按一个中型团队每天 2000 条估算，一年 70 万条、磁盘几十 MB —— 单表本身扛得住，
 * 但「审计日志」页面默认查 30 天，老数据既不会被展示也不会被清理，属于纯膨胀。
 *
 * <p>策略（保守优先，不删近数据）：
 * <ul>
 *   <li>{@code qws.audit.retention-days}（默认 180 天）：超过这个天数的记录才会被清理；</li>
 *   <li>每天凌晨 3:17 跑一次清理（避开业务高峰，非整点减少撞车）；</li>
 *   <li>单次最多删 {@code max-delete-per-run}（默认 5000）条，避免一次大事务锁表；</li>
 *   <li>只删「成功」的旧记录，<b>失败记录永久保留</b>——安全事件需要长期可追溯。</li>
 * </ul>
 *
 * <p>为什么不做"归档到另一张表"：单库单表场景下归档只增加复杂度（两张表都要维护索引、
 * 查询要 union），而真正需要长期留存的是失败/异常记录（已排除在清理之外）。
 * 若将来确实需要合规留存，再把 {@link #cleanup(int, int)} 的删除改成迁移即可。
 */
@Service
public class AuditLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionService.class);

    private final SysAuditLogMapper auditLogMapper;

    /** 保留天数；<= 0 表示永久保留（关闭自动清理） */
    @Value("${qws.audit.retention-days:180}")
    private int retentionDays;

    /** 单次清理上限，防止一条 DELETE 锁表过久 */
    @Value("${qws.audit.max-delete-per-run:5000}")
    private int maxDeletePerRun;

    public AuditLogRetentionService(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 定时清理。默认每天 03:17。
     *
     * <p>用 {@code fixedDelay} 语义的 cron 而非 {@code fixedRate}：本轮跑完再等下一轮，
     * 不会因为某次执行过久导致任务堆积。
     */
    @Scheduled(cron = "${qws.audit.cleanup-cron:0 17 3 * * ?}")
    public void scheduledCleanup() {
        if (retentionDays <= 0) {
            log.debug("审计日志保留策略已关闭（retention-days={}），跳过清理", retentionDays);
            return;
        }
        try {
            Map<String, Object> result = cleanup(retentionDays, maxDeletePerRun);
            log.info("审计日志清理完成 {}", result);
        } catch (Exception ex) {
            // 清理失败不能影响服务：只记日志，等下次调度重试
            log.warn("审计日志清理失败：{}", ex.getMessage(), ex);
        }
    }

    /**
     * 执行一次清理。
     *
     * @param days  保留天数（超过该天数的记录才删）
     * @param limit 单次最多删除条数
     * @return 清理结果统计
     */
    @Transactional
    public Map<String, Object> cleanup(int days, int limit) {
        if (days <= 0) {
            throw new IllegalArgumentException("保留天数必须大于 0");
        }
        int batch = limit <= 0 ? 5000 : limit;
        LocalDateTime cutoff = LocalDate.now().minusDays(days).atStartOfDay();

        // 只删成功记录：fail / pending 一律保留（安全事件追溯）
        long candidates = auditLogMapper.selectCount(new LambdaQueryWrapper<SysAuditLog>()
                .lt(SysAuditLog::getOperateTime, cutoff)
                .eq(SysAuditLog::getStatus, "success"));

        int deleted = 0;
        if (candidates > 0) {
            // 先按 id 取一批要删的，再按 id 删，避免 LIMIT 在 DELETE 里的方言差异
            var ids = auditLogMapper.selectList(new LambdaQueryWrapper<SysAuditLog>()
                            .select(SysAuditLog::getId)
                            .lt(SysAuditLog::getOperateTime, cutoff)
                            .eq(SysAuditLog::getStatus, "success")
                            .orderByAsc(SysAuditLog::getId)
                            .last("LIMIT " + batch))
                    .stream().map(SysAuditLog::getId).toList();
            if (!ids.isEmpty()) {
                deleted = auditLogMapper.deleteBatchIds(ids);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cutoff", cutoff.toString());
        result.put("retentionDays", days);
        result.put("candidates", candidates);
        result.put("deleted", deleted);
        result.put("remaining", auditLogMapper.selectCount(null));
        return result;
    }

    /** 当前策略（供前端/运维查询） */
    public Map<String, Object> policy() {
        Map<String, Object> result = new HashMap<>();
        result.put("retentionDays", retentionDays);
        result.put("maxDeletePerRun", maxDeletePerRun);
        result.put("keepFailedRecords", true);
        result.put("enabled", retentionDays > 0);
        result.put("total", auditLogMapper.selectCount(null));
        return result;
    }
}
