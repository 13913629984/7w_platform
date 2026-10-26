package com.qws.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码哈希回归测试。
 *
 * <p>覆盖第 3 批的关键改造：从无盐 SHA-256 迁到 BCrypt，
 * 且必须兼容存量旧密码（否则所有老用户会被踢下线）。
 */
class PasswordUtilTest {

    private static final String PASSWORD = "Test1234";

    @Test
    @DisplayName("新密码使用 BCrypt（$2a$ 前缀，长度 60）")
    void hashUsesBcrypt() {
        String hashed = PasswordUtil.hash(PASSWORD);
        assertTrue(hashed.startsWith("$2a$") || hashed.startsWith("$2b$"), "应为 BCrypt 格式");
        assertEquals(60, hashed.length(), "BCrypt 哈希固定 60 字符");
        assertTrue(PasswordUtil.isBcrypt(hashed));
    }

    @Test
    @DisplayName("同一密码两次哈希不同（BCrypt 随机盐）")
    void hashIsSalted() {
        String first = PasswordUtil.hash(PASSWORD);
        String second = PasswordUtil.hash(PASSWORD);
        assertNotEquals(first, second, "有随机盐时两次哈希必须不同");
        // 但两者都能校验通过
        assertTrue(PasswordUtil.matches(PASSWORD, first));
        assertTrue(PasswordUtil.matches(PASSWORD, second));
    }

    @Test
    @DisplayName("BCrypt 校验：正确/错误密码")
    void matchesBcrypt() {
        String hashed = PasswordUtil.hash(PASSWORD);
        assertTrue(PasswordUtil.matches(PASSWORD, hashed));
        assertFalse(PasswordUtil.matches("wrong", hashed));
        assertFalse(PasswordUtil.matches("", hashed));
        assertFalse(PasswordUtil.matches(null, hashed));
    }

    @Test
    @DisplayName("兼容历史无盐 SHA-256（老用户仍能登录）")
    void matchesLegacySha256() {
        // sha256("123456")
        String legacy = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        assertTrue(PasswordUtil.matches("123456", legacy));
        assertFalse(PasswordUtil.matches("1234567", legacy));
        assertFalse(PasswordUtil.isBcrypt(legacy));
    }

    @Test
    @DisplayName("旧格式需要升级、新格式不需要")
    void needsUpgrade() {
        String legacy = PasswordUtil.legacySha256(PASSWORD);
        assertTrue(PasswordUtil.needsUpgrade(legacy));
        assertFalse(PasswordUtil.needsUpgrade(PasswordUtil.hash(PASSWORD)));
        assertFalse(PasswordUtil.needsUpgrade(null));
        assertFalse(PasswordUtil.needsUpgrade(""));
    }

    @Test
    @DisplayName("认不出的哈希格式一律拒绝（不能跳过校验放行）")
    void unknownFormatRejected() {
        assertFalse(PasswordUtil.matches(PASSWORD, "not-a-hash"));
        assertFalse(PasswordUtil.matches(PASSWORD, ""));
        assertFalse(PasswordUtil.matches(PASSWORD, null));
    }

    @Test
    @DisplayName("空密码不允许哈希")
    void emptyPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
    }
}
