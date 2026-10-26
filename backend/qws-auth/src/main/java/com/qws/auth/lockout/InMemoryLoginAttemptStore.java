package com.qws.auth.lockout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版登录失败存储（默认实现）。
 *
 * <p>适用：单实例部署。零依赖、无网络开销，重启后计数丢失（可接受——
 * 重启本身就等于给所有人解锁一次，且攻击者无法控制重启时机）。
 *
 * <p>内存泄漏防护：每个被尝试过的账号都会留一条记录，长期运行会缓慢增长。
 * 用 {@link #purgeExpired()} 定时清理「锁定已过期且失败次数已重置」的条目。
 */
@Component
public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLoginAttemptStore.class);

    /** 失败次数保留窗口：超过这个时间没再失败，计数归零（避免十天前的 4 次失败 + 今天的 1 次就锁号） */
    private static final Duration FAILURE_WINDOW = Duration.ofHours(1);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** 单条记录：失败次数 + 首次失败时间 + 锁定截止时间 */
    private record Attempt(int failures, LocalDateTime firstFailureAt, LocalDateTime lockedUntil) {
    }

    @Override
    public Duration lockedFor(String account) {
        Attempt attempt = attempts.get(account);
        if (attempt == null || attempt.lockedUntil() == null) {
            return Duration.ZERO;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(attempt.lockedUntil())) {
            return Duration.between(now, attempt.lockedUntil());
        }
        // 锁定已过期，顺手清掉
        attempts.remove(account);
        return Duration.ZERO;
    }

    @Override
    public int recordFailure(String account, int maxFailures, Duration lockDuration) {
        LocalDateTime now = LocalDateTime.now();
        Attempt result = attempts.compute(account, (key, previous) -> {
            // 超出计数窗口则重新开始计数
            boolean expired = previous == null
                    || previous.firstFailureAt().isBefore(now.minus(FAILURE_WINDOW));
            int failures = expired ? 1 : previous.failures() + 1;
            LocalDateTime firstFailureAt = expired ? now : previous.firstFailureAt();
            LocalDateTime lockedUntil = failures >= maxFailures ? now.plus(lockDuration) : null;
            return new Attempt(failures, firstFailureAt, lockedUntil);
        });
        if (result.lockedUntil() != null) {
            log.warn("账号 {} 连续登录失败 {} 次，已锁定至 {}", account, result.failures(), result.lockedUntil());
        }
        return result.failures();
    }

    @Override
    public void clear(String account) {
        attempts.remove(account);
    }

    @Override
    public int failureCount(String account) {
        Attempt attempt = attempts.get(account);
        return attempt == null ? 0 : attempt.failures();
    }

    /**
     * 定时清理：每 10 分钟一次。
     *
     * <p>清理条件：锁定已过期（或从未锁定）且首次失败时间已超出计数窗口。
     * 锁定期内的条目绝对不动。
     */
    @Override
    @Scheduled(fixedDelay = 600_000L, initialDelay = 60_000L)
    public int purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minus(FAILURE_WINDOW);
        int removed = 0;
        Iterator<Map.Entry<String, Attempt>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            Attempt attempt = iterator.next().getValue();
            boolean locked = attempt.lockedUntil() != null && now.isBefore(attempt.lockedUntil());
            if (!locked && attempt.firstFailureAt().isBefore(windowStart)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("登录失败记录清理 {} 条，剩余 {} 条", removed, attempts.size());
        }
        return removed;
    }

    /** 当前条目数（监控用） */
    public int size() {
        return attempts.size();
    }
}
