package com.qws.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * 密码哈希与校验。
 *
 * <p>历史实现是无盐 SHA-256（{@link #legacySha256}），存在两个问题：
 * <ol>
 *   <li>相同密码哈希值相同，一次彩虹表就能批量撞库；</li>
 *   <li>没有工作因子，GPU 每秒可验证数十亿次。</li>
 * </ol>
 *
 * <p>现在改用 BCrypt（自带随机盐 + 可调工作因子）。为了不把所有老用户踢下线，
 * 采用<b>渐进式升级</b>：
 * <ul>
 *   <li>{@link #matches} 同时认 BCrypt 和旧 SHA-256；</li>
 *   <li>登录成功后由调用方检查 {@link #needsUpgrade}，是旧格式就顺便重写成 BCrypt。</li>
 * </ul>
 * 这样老用户下次登录时密码自动升级，不需要重置密码。
 *
 * <p>BCrypt 哈希固定以 {@code $2a$} / {@code $2b$} / {@code $2y$} 开头，
 * 据此可以无歧义地区分两种格式（旧 SHA-256 是 64 位十六进制）。
 */
public final class PasswordUtil {

    /** BCrypt 强度：10 轮约 50~100ms，兼顾安全与登录体验 */
    private static final int BCRYPT_STRENGTH = 10;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.*$");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private PasswordUtil() {
    }

    /** 生成新密码哈希（BCrypt，自带盐） */
    public static String hash(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return ENCODER.encode(password);
    }

    /**
     * 校验密码。同时支持 BCrypt 与历史 SHA-256。
     *
     * @param rawPassword    明文密码
     * @param storedPassword 库里的哈希
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        if (LEGACY_PATTERN.matcher(storedPassword).matches()) {
            // 定长比较，避免时序侧信道
            byte[] expected = storedPassword.getBytes(StandardCharsets.UTF_8);
            byte[] actual = legacySha256(rawPassword).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, actual);
        }
        // 认不出的格式一律拒绝，不能"跳过校验"放行
        return false;
    }

    /** 是否为 BCrypt 格式 */
    public static boolean isBcrypt(String storedPassword) {
        return storedPassword != null && BCRYPT_PATTERN.matcher(storedPassword).matches();
    }

    /**
     * 是否需要升级成 BCrypt。
     *
     * <p>调用时机：<b>密码校验通过之后</b>。是旧格式就重写数据库里的值。
     */
    public static boolean needsUpgrade(String storedPassword) {
        return StringUtils.hasText(storedPassword) && !isBcrypt(storedPassword);
    }

    /**
     * 历史无盐 SHA-256。仅用于校验存量密码，<b>不要用于新密码</b>。
     */
    static String legacySha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
