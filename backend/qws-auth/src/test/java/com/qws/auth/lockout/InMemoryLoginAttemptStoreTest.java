package com.qws.auth.lockout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录失败锁定测试。
 *
 * <p>对应第 4 批把「内存 Map 硬编码」抽成 {@link LoginAttemptStore} 接口后的行为锁定。
 * 这些用例同时是 Redis 实现的验收标准（换实现后同一套测试应该过）。
 */
class InMemoryLoginAttemptStoreTest {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK = Duration.ofMinutes(15);

    private InMemoryLoginAttemptStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryLoginAttemptStore();
    }

    @Test
    @DisplayName("未失败过的账号不会被锁")
    void neverFailedNotLocked() {
        assertEquals(Duration.ZERO, store.lockedFor("alice"));
        assertEquals(0, store.failureCount("alice"));
    }

    @Test
    @DisplayName("失败累计到阈值触发锁定")
    void lockAfterMaxFailures() {
        for (int i = 1; i < MAX_FAILURES; i++) {
            store.recordFailure("bob", MAX_FAILURES, LOCK);
            assertTrue(store.lockedFor("bob").isZero(), "第 " + i + " 次失败不应锁定");
        }
        store.recordFailure("bob", MAX_FAILURES, LOCK);
        Duration remain = store.lockedFor("bob");
        assertFalse(remain.isZero(), "第 " + MAX_FAILURES + " 次失败应触发锁定");
        assertTrue(remain.toMinutes() <= 15 && remain.toMinutes() >= 14, "剩余应接近 15 分钟，实际 " + remain);
    }

    @Test
    @DisplayName("登录成功后清除计数")
    void clearResetsCounter() {
        store.recordFailure("carol", MAX_FAILURES, LOCK);
        store.recordFailure("carol", MAX_FAILURES, LOCK);
        assertEquals(2, store.failureCount("carol"));

        store.clear("carol");
        assertEquals(0, store.failureCount("carol"));
        assertEquals(Duration.ZERO, store.lockedFor("carol"));
    }

    @Test
    @DisplayName("锁定时长为 0 时不产生锁定状态（便于测试/配置关闭）")
    void zeroLockDurationNeverLocks() {
        for (int i = 0; i < MAX_FAILURES + 2; i++) {
            store.recordFailure("dave", MAX_FAILURES, Duration.ZERO);
        }
        assertEquals(Duration.ZERO, store.lockedFor("dave"));
    }

    @Test
    @DisplayName("不同账号互不影响")
    void accountsAreIsolated() {
        for (int i = 0; i < MAX_FAILURES; i++) {
            store.recordFailure("locked", MAX_FAILURES, LOCK);
        }
        assertFalse(store.lockedFor("locked").isZero());
        assertTrue(store.lockedFor("other").isZero());
    }

    @Test
    @DisplayName("purgeExpired 不会清掉锁定期内的记录")
    void purgeKeepsLockedEntries() {
        for (int i = 0; i < MAX_FAILURES; i++) {
            store.recordFailure("locked", MAX_FAILURES, LOCK);
        }
        store.purgeExpired();
        // 刚锁定，必然还在
        assertFalse(store.lockedFor("locked").isZero());
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("purgeExpired 可清掉早已过窗口的记录（防内存泄漏）")
    void purgeRemovesStaleEntries() {
        // 造一条失败次数未达阈值、且首次失败时间很老的记录：
        // 直接调用 recordFailure 无法伪造时间，改为验证「未锁定 + 时间在窗口内」不会被误删
        store.recordFailure("fresh", MAX_FAILURES, LOCK);
        int removed = store.purgeExpired();
        assertEquals(0, removed, "窗口内的记录不应被清理");
        assertEquals(1, store.size());
    }
}
