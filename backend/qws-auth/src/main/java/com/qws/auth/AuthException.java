package com.qws.auth;

/**
 * 认证失败。
 *
 * <p>带 HTTP 状态码：登录失败是 401（凭证不对），未登录/凭证无效也是 401。
 * 之前统一返回 HTTP 200 + code 500，导致前端无法区分「未登录」和「token 过期」，
 * 也无法用 axios 拦截器统一处理。
 */
public class AuthException extends RuntimeException {

    private final int status;

    public AuthException(String message) {
        this(message, 401);
    }

    public AuthException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
