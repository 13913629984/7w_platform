package com.qws.common.security;

import java.util.List;

/**
 * 权限码的分层解析工具。
 *
 * <p>平台里有两种粒度的权限码：
 * <ul>
 *   <li><b>子系统级</b>（{@code WMS} / {@code CRM} / {@code FIN} ...）——表示"可以进入该子系统"；</li>
 *   <li><b>操作级</b>（{@code WMS_VIEW} / {@code WMS_INBOUND} ...）——表示"能做某个具体动作"。</li>
 * </ul>
 *
 * <p>历史上角色种子数据配的是操作级码（{@code WMS_VIEW}），而 Controller 上标的是子系统级码
 * （{@code @RequirePermission("WMS")}），两边对不上导致"配了权限却全线 403"。
 * 这里集中定义两者的换算与只读判定，供 {@link UserContext} 与网关/拦截器共用，
 * 避免每个模块各写一套。
 */
public final class PermissionCodes {

    /** 操作级权限码的分隔符，也是"子系统级 vs 操作级"的判定依据。 */
    public static final String OPERATION_SEPARATOR = "_";

    /**
     * 被视为"只读"的权限后缀。
     *
     * <p>只读码（{@code WMS_VIEW} / {@code SYS_AUDIT}）只能通过 GET 请求，
     * 不能再用来发起写操作——否则"查看库存"就等于"可以执行出库"。
     */
    private static final List<String> READ_ONLY_SUFFIXES = List.of("_VIEW", "_AUDIT");

    private PermissionCodes() {
    }

    /** 取权限码所属的子系统（{@code WMS_OUTBOUND} -> {@code WMS}；{@code WMS} -> {@code WMS}）。 */
    public static String subsystemOf(String code) {
        if (code == null) return "";
        int index = code.indexOf(OPERATION_SEPARATOR);
        return index < 0 ? code : code.substring(0, index);
    }

    /** 是否为子系统级权限码（不含下划线）。 */
    public static boolean isSubsystemLevel(String code) {
        return code != null && !code.isBlank() && code.indexOf(OPERATION_SEPARATOR) < 0;
    }

    /** 是否为只读权限码（以 _VIEW / _AUDIT 结尾）。 */
    public static boolean isReadOnly(String code) {
        if (code == null) return false;
        String upper = code.toUpperCase();
        return READ_ONLY_SUFFIXES.stream().anyMatch(upper::endsWith);
    }

    /** 角色码格式：大写字母开头，只允许大写字母/数字/下划线。用于拒绝"普通用户"这类中文角色名进 token。 */
    public static boolean isWellFormedRoleCode(String code) {
        return code != null && code.matches("^[A-Z][A-Z0-9_]{0,63}$");
    }
}
