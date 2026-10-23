package com.qws.gateway;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 内部请求签名（网关侧副本）。
 *
 * <p>为什么不在网关直接用 {@code qws-common} 里的同名工具类？
 * 因为 qws-common 依赖 {@code spring-boot-starter-web}（Servlet 栈），
 * 而网关是 WebFlux 响应式栈，两者同时出现在 classpath 会导致
 * Spring Cloud Gateway 启动失败。所以这里保留一份算法完全一致的精简实现，
 * 两侧只要 payload 拼接顺序一致即可互验。
 */
final class InternalSignature {

    static final String HEADER = "X-Internal-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    private InternalSignature() {
    }

    static String compute(String secret, String userId, String username, String roles, String permissions) {
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
