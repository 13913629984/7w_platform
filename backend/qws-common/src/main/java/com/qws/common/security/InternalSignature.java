package com.qws.common.security;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部请求签名校验。
 *
 * <p>网关校验 JWT 后会注入 {@code X-User-*} 头，但这些头本身没有任何防伪能力——
 * 如果微服务端口 809x 能被直连，攻击者只要手写 {@code X-User-Roles: SUPER_ADMIN}
 * 就能冒充管理员（实测确认过）。
 *
 * <p>因此网关额外注入一个用共享密钥对所有身份头计算的 HMAC 签名
 * （{@code X-Internal-Signature}），微服务收到后重算比对。直连方不知道密钥，
 * 无法伪造出合法签名，请求会被拒绝。
 *
 * <p>密钥通过配置项 {@code qws.security.secret} 下发，网关与所有微服务保持一致。
 */
public final class InternalSignature {

    public static final String HEADER = "X-Internal-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    private InternalSignature() {
    }

    /**
     * 对身份信息计算签名。参与签名的字段顺序必须与网关完全一致。
     */
    public static String compute(String secret, String userId, String username,
                                 String roles, String permissions) {
        String payload = nullToEmpty(userId) + "\n"
                + nullToEmpty(username) + "\n"
                + nullToEmpty(roles) + "\n"
                + nullToEmpty(permissions);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("计算内部请求签名失败", ex);
        }
    }

    /** 校验请求携带的内部签名是否合法。 */
    public static boolean verify(String secret, HttpServletRequest request) {
        String provided = request.getHeader(HEADER);
        if (provided == null || provided.isBlank()) {
            return false;
        }
        String expected = compute(secret,
                request.getHeader(UserContextInterceptor.HEADER_USER_ID),
                request.getHeader(UserContextInterceptor.HEADER_USERNAME),
                request.getHeader(UserContextInterceptor.HEADER_ROLES),
                request.getHeader(UserContextInterceptor.HEADER_PERMISSIONS));
        // 定长比较，避免时序侧信道
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
