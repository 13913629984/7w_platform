package com.qws.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写审计日志的接口。
 *
 * <p>由 {@link AuditAspect} 拦截并写 {@code sys_audit_log}。设计上刻意<b>不</b>要求
 * 业务方法自己做任何事——打注解即可，既不侵入业务代码，也不会因为漏写而丢日志。
 *
 * <p>用法：
 * <pre>{@code
 * @PostMapping("/create")
 * @Audit(module = "CRM", action = ActionType.CREATE, target = "客户")
 * public ApiResult<CustomerVO> create(@RequestBody CustomerRequest request) { ... }
 * }</pre>
 *
 * <p>日志内容自动拼接：{@code 用户名 动作 对象}，例如「admin 新增 客户」。
 * 失败时（抛异常或返回 code != 0）会记 status=fail 并带上错误信息。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 模块码，与前端审计页的筛选值一致：SYS / CRM / WMS / MAINT / ETS / FIN */
    String module();

    /** 操作类型 */
    ActionType action();

    /** 操作对象名称，例如「客户」「出库单」。用于拼「admin 新增 客户」 */
    String target();

    /**
     * 是否记录请求参数。
     * 涉及密码等敏感字段时置 false，避免把明文密码写进审计表。
     */
    boolean recordPayload() default true;

    /** 模块中文名，不填则按 {@link #module()} 映射 */
    String moduleName() default "";

    /**
     * 从请求参数里读用户名的 JSON 字段名。
     *
     * <p>登录接口没有登录上下文（UserContext 为空），审计日志会退化成 "system"，
     * 这对「谁在尝试登录」这种关键审计场景毫无价值。
     * 填上这个属性（如 {@code "username"}）后，切面会从第一个参数对象里取该字段作为操作人。
     */
    String usernameKey() default "";
}
