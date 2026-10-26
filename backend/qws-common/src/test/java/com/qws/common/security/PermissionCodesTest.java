package com.qws.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限判定回归测试。
 *
 * <p>这些用例直接对应第 2 批修掉的真实缺陷：
 * 角色配的是 {@code WMS_VIEW}、Controller 标的是 {@code WMS}，
 * 两边粒度不同导致"配了权限却全线 403"。
 */
class PermissionCodesTest {

    @Test
    @DisplayName("子系统码解析：WMS_OUTBOUND -> WMS，WMS -> WMS")
    void subsystemOf() {
        assertEquals("WMS", PermissionCodes.subsystemOf("WMS_OUTBOUND"));
        assertEquals("WMS", PermissionCodes.subsystemOf("WMS"));
        assertEquals("", PermissionCodes.subsystemOf(null));
        assertEquals("SYS", PermissionCodes.subsystemOf("SYS_AUDIT"));
    }

    @Test
    @DisplayName("只读码识别：_VIEW / _AUDIT 结尾")
    void readOnly() {
        assertTrue(PermissionCodes.isReadOnly("WMS_VIEW"));
        assertTrue(PermissionCodes.isReadOnly("SYS_AUDIT"));
        assertFalse(PermissionCodes.isReadOnly("WMS_OUTBOUND"));
        assertFalse(PermissionCodes.isReadOnly("WMS"));
        assertFalse(PermissionCodes.isReadOnly(null));
    }

    @Test
    @DisplayName("角色码格式：拒绝中文角色名（它会撞爆 HTTP header）")
    void roleCodeFormat() {
        assertTrue(PermissionCodes.isWellFormedRoleCode("CRM_SALES"));
        assertTrue(PermissionCodes.isWellFormedRoleCode("STAFF"));
        assertFalse(PermissionCodes.isWellFormedRoleCode("普通用户"));
        assertFalse(PermissionCodes.isWellFormedRoleCode("crm_sales"));
        assertFalse(PermissionCodes.isWellFormedRoleCode("1ABC"));
        assertFalse(PermissionCodes.isWellFormedRoleCode(null));
    }

    // ---------------- UserContext 三级判定 ----------------

    private UserContext ctx(String... permissions) {
        return new UserContext("1", "tester", List.of("STAFF"), List.of(permissions), false);
    }

    private UserContext writeCtx(String... permissions) {
        return new UserContext("1", "tester", List.of("STAFF"), List.of(permissions), true);
    }

    @Test
    @DisplayName("需要子系统级：持有任意子系统下操作码也放行")
    void subsystemGrantedByOperationCode() {
        assertTrue(ctx("WMS_VIEW").hasPermission("WMS"));
        assertTrue(ctx("WMS_OUTBOUND").hasPermission("WMS"));
        assertFalse(ctx("CRM_VIEW").hasPermission("WMS"));
    }

    @Test
    @DisplayName("需要操作级：被子系统级权限覆盖")
    void operationGrantedBySubsystem() {
        assertTrue(ctx("WMS").hasPermission("WMS_INBOUND"));
        assertFalse(ctx("CRM").hasPermission("WMS_INBOUND"));
    }

    @Test
    @DisplayName("同子系统内操作码不可互替：CRM_VIEW 不等于 CRM_DELETE")
    void operationCodesNotInterchangeable() {
        assertFalse(ctx("CRM_VIEW").hasPermission("CRM_DELETE"));
        assertTrue(ctx("CRM_VIEW").hasPermission("CRM_VIEW"));
    }

    @Test
    @DisplayName("只读权限对读请求有效")
    void readOnlyAllowsRead() {
        assertTrue(ctx("WMS_VIEW").hasPermission("WMS"));
    }

    @Test
    @DisplayName("只读权限不能用于写操作（WMS_VIEW 不能执行出库）")
    void readOnlyDeniedForWrite() {
        assertFalse(writeCtx("WMS_VIEW").hasWritePermission("WMS", null));
        assertFalse(writeCtx("SYS_AUDIT").hasWritePermission("SYS", null));
        // 有任一非只读操作码就有写权限
        assertTrue(writeCtx("WMS_VIEW", "WMS_OUTBOUND").hasWritePermission("WMS", null));
        // 子系统级权限视为可写
        assertTrue(writeCtx("WMS").hasWritePermission("WMS", null));
    }

    @Test
    @DisplayName("管理员角色放行所有判定")
    void adminBypass() {
        UserContext admin = new UserContext("1", "admin", List.of("SUPER_ADMIN"), List.of(), true);
        assertTrue(admin.hasPermission("WMS_DELETE"));
        assertTrue(admin.hasWritePermission("ANYTHING", null));
        assertTrue(admin.isAdmin());
    }

    @Test
    @DisplayName("空权限码视为不需要权限")
    void blankCodeAlwaysAllowed() {
        assertTrue(ctx().hasPermission(""));
        assertTrue(ctx().hasPermission(null));
    }
}
