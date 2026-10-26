package com.qws.auth.lockout;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 登录失败计数与锁定状态存储。
 *
 * <p>为什么抽象成接口：第 3 批用 {@code ConcurrentHashMap} 实现，单实例部署没问题，
 * 但一旦 auth 服务多副本（K8s 滚动更新、负载均衡），攻击者只要把请求打到不同实例
 * 就能绕过"5 次锁定"——每个实例各记各的。
 *
 * <p>默认仍用内存实现（零依赖、开箱即用）；需要多实例时把
 * {@code qws.auth.login-lock.store} 配成 {@code redis} 即可切到 Redis 实现。
 *
 * <p>接口刻意只用简单类型（String / int / Duration），不暴露内部结构，
 * 便于两种实现各自选择存储格式（内存存对象、Redis 存 hash）。
 */
public interface LoginAttemptStore {

    /**
     * 查询锁定状态。
     *
     * @param account 账号（已 trim）
     * @return 若处于锁定期，返回剩余时长；否则返回 {@link Duration#ZERO}
     */
    Duration lockedFor(String account);

    /**
     * 记录一次失败，并返回累计失败次数。
     *
     * <p>达到阈值时实现方应同时设置锁定截止时间。
     *
     * @param account 账号
     * @param maxFailures 触发锁定的失败次数阈值
     * @param lockDuration 锁定时长
     * @return 记录后的累计失败次数
     */
    int recordFailure(String account, int maxFailures, Duration lockDuration);

    /** 登录成功后清除该账号的失败记录 */
    void clear(String account);

    /** 当前已累计的失败次数（仅用于日志/测试） */
    int failureCount(String account);

    /**
     * 清理已过期、不再需要的记录。
     *
     * <p>内存实现需要它防止 Map 无限增长（每个被尝试过的账号都会留一条）；
     * Redis 实现依赖 TTL 自动过期，可以空实现。
     *
     * @return 清理掉的条目数
     */
    int purgeExpired();
}
