package com.qws.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明接口所需的权限码，由 {@link UserContextInterceptor} 统一校验。
 *
 * <p>可标在类上（作用于全部方法）或方法上（覆盖类级声明）。
 * 示例：{@code @RequirePermission("WMS")} 表示需要 WMS 子系统权限。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 需要的权限码，多个之间为"与"关系。 */
    String[] value();

    /** 是否允许匿名访问（默认不允许）。 */
    boolean allowAnonymous() default false;
}
