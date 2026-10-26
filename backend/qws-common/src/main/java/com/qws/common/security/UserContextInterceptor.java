package com.qws.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 微服务侧授权拦截器。
 *
 * <p>网关已完成 JWT 校验并注入 {@code X-User-*} 请求头，本拦截器负责：
 * <ol>
 *   <li>校验内部请求签名，防止绕过网关直连微服务伪造身份头；</li>
 *   <li>把请求头还原成 {@link UserContext}，供业务代码读取当前用户；</li>
 *   <li>校验 {@link RequirePermission} 声明的权限码，不足则返回 403。</li>
 * </ol>
 */
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_ROLES = "X-User-Roles";
    public static final String HEADER_PERMISSIONS = "X-User-Permissions";

    private final String secret;
    private final boolean requireSignature;

    public UserContextInterceptor(String secret, boolean requireSignature) {
        this.secret = secret;
        this.requireSignature = requireSignature;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        // actuator 探活不需要身份
        if (path.startsWith("/actuator")) {
            return true;
        }

        if (requireSignature && !InternalSignature.verify(secret, request)) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "缺少或非法的内部请求签名，请通过网关访问");
            return false;
        }

        UserContext context = new UserContext(
                request.getHeader(HEADER_USER_ID),
                decode(request.getHeader(HEADER_USERNAME)),
                split(request.getHeader(HEADER_ROLES)),
                split(request.getHeader(HEADER_PERMISSIONS)),
                isWrite(request));
        UserContext.set(context);

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequirePermission required = resolve(method);
        if (required == null || required.allowAnonymous()) {
            return true;
        }

        for (String code : required.value()) {
            boolean allowed = context.isWriteRequest()
                    ? context.hasWritePermission(code, null)
                    : context.hasPermission(code);
            if (!allowed) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, context.describeDenied(code));
                return false;
            }
        }
        return true;
    }

    /** 写请求判定：非 GET/HEAD/OPTIONS 都算写 */
    private static boolean isWrite(HttpServletRequest request) {
        String httpMethod = request.getMethod();
        return !("GET".equalsIgnoreCase(httpMethod)
                || "HEAD".equalsIgnoreCase(httpMethod)
                || "OPTIONS".equalsIgnoreCase(httpMethod));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理，避免 Tomcat 线程复用导致上一个请求的用户串到下一个请求
        UserContext.clear();
    }

    private RequirePermission resolve(HandlerMethod method) {
        RequirePermission onMethod = method.getMethodAnnotation(RequirePermission.class);
        if (onMethod != null) return onMethod;
        return method.getBeanType().getAnnotation(RequirePermission.class);
    }

    private static List<String> split(String header) {
        if (header == null || header.isBlank()) return List.of();
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                // 网关会把含非 ASCII 的角色名/用户名做 percent-encode（HTTP header 只能是 ASCII），
                // 这里编码回来，业务代码拿到的才是原始值。
                .map(UserContextInterceptor::decode)
                .toList();
    }

    /** percent-decode；纯 ASCII 原样返回，不抛异常（旧数据/未编码值也兼容） */
    private static String decode(String value) {
        if (value == null || value.indexOf('%') < 0) return value;
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private void reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
