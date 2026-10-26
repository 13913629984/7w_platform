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
    /** 本次请求是否写操作（GET/HEAD/OPTIONS 视为读），用于只读权限的降级判定 */
    private final boolean writeRequest;

    public UserContext(String userId, String username, List<String> roles, List<String> permissions) {
        this(userId, username, roles, permissions, false);
    }

    public UserContext(String userId, String username, List<String> roles, List<String> permissions,
                       boolean writeRequest) {
        this.userId = userId;
        this.username = username;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
        this.writeRequest = writeRequest;
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
    public boolean isWriteRequest() { return writeRequest; }

    /** 管理员角色直接放行所有权限判断。 */
    public boolean isAdmin() {
        return roles.stream().anyMatch(role ->
                "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "SYS_ADMIN".equals(role));
    }

    /**
     * 判断是否具备某个权限码。
     *
     * <p>三级判定，解决"角色配的码和注解标的码粒度不同"的历史问题：
     * <ol>
     *   <li>精确命中：所需码本身在权限列表里；</li>
     *   <li>子系统覆盖操作：需要 {@code WMS} 时，持有任意 {@code WMS_*} 也放行；</li>
     *   <li>操作被子系统覆盖：需要 {@code WMS_INBOUND} 时，持有 {@code WMS} 也放行。</li>
     * </ol>
     *
     * <p>只读权限（{@code _VIEW} / {@code _AUDIT}）只对读请求生效：
     * 持有 {@code WMS_VIEW} 不能发起"执行出库"这类写操作，必须另有写权限。
     */
    public boolean hasPermission(String code) {
        if (code == null || code.isBlank()) return true;
        if (isAdmin()) return true;
        if (permissions.contains(code)) return true;

        String subsystem = PermissionCodes.subsystemOf(code);
        // 1) 需要子系统级：持有该子系统下任一操作级也放行
        if (PermissionCodes.isSubsystemLevel(code)) {
            return permissions.stream().anyMatch(p -> subsystem.equals(PermissionCodes.subsystemOf(p)));
        }
        // 2) 需要操作级：被子系统级权限覆盖
        if (permissions.contains(subsystem)) {
            return true;
        }
        // 3) 同子系统内其他操作级不能替代（例如 CRM_VIEW 不能代替 CRM_DELETE）
        return false;
    }

    /**
     * 写操作的权限判定：在 {@link #hasPermission(String)} 基础上，
     * 排除"只靠只读权限通过"的情况。
     *
     * @param subsystemCode 子系统码（WMS / CRM / ...）
     * @param operationCode 操作码（WMS_OUTBOUND / ...），可为 null 表示只要求子系统写权限
     */
    public boolean hasWritePermission(String subsystemCode, String operationCode) {
        if (isAdmin()) return true;
        if (subsystemCode == null || subsystemCode.isBlank()) return true;

        // 显式持有写操作码
        if (operationCode != null && permissions.contains(operationCode)) return true;
        if (permissions.contains(subsystemCode)) return true;

        // 同子系统下持有任意"非只读"操作码也视为有写权限
        return permissions.stream()
                .filter(p -> subsystemCode.equals(PermissionCodes.subsystemOf(p)))
                .anyMatch(p -> !PermissionCodes.isReadOnly(p));
    }

    /** 仅凭只读权限访问写接口时的提示文案 */
    public String describeDenied(String code) {
        return "没有访问权限（缺少权限码 " + code + "）";
    }
}
