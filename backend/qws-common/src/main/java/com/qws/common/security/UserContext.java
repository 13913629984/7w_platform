package com.qws.common.security;

import java.util.List;

/**
 * 当前请求的登录用户上下文。
 *
 * <p>由 {@link UserContextInterceptor} 从网关注入的请求头解析后写入 ThreadLocal，
 * 业务代码可通过 {@link #get()} 拿到用户名 / 角色 / 权限，做数据范围与操作权限判断。
 * 请求结束后必须由拦截器 {@link #clear()}，否则线程复用会串数据。
 */
public final class UserContext {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private final String userId;
    private final String username;
    private final List<String> roles;
    private final List<String> permissions;

    public UserContext(String userId, String username, List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static String currentUsername() {
        UserContext context = HOLDER.get();
        return context == null ? null : context.getUsername();
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public List<String> getRoles() { return roles; }
    public List<String> getPermissions() { return permissions; }

    /** 管理员角色直接放行所有权限判断。 */
    public boolean isAdmin() {
        return roles.stream().anyMatch(role ->
                "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "SYS_ADMIN".equals(role));
    }

    /**
     * 判断是否具备某个权限码。
     *
     * <p>支持两种粒度：子系统级（如 {@code WMS}）与操作级（如 {@code WMS_MATERIAL}）。
     * 持有子系统级权限即视为覆盖其下所有操作级权限。
     */
    public boolean hasPermission(String code) {
        if (code == null || code.isBlank()) return true;
        if (isAdmin()) return true;
        if (permissions.contains(code)) return true;
        // WMS_MATERIAL 可由 WMS 覆盖
        return permissions.stream().anyMatch(p -> code.startsWith(p + "_"));
    }
}
