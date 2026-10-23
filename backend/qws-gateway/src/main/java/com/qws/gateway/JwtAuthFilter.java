package com.qws.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 网关统一 JWT 鉴权。
 *
 * <p>校验通过后把用户身份写入下游请求头（X-User-Id / X-Username / X-User-Roles / X-User-Permissions），
 * 微服务可据此做数据范围与操作权限控制。网关属于响应式栈，不能用 Servlet 的 OncePerRequestFilter。
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_ROLES = "X-User-Roles";
    public static final String HEADER_PERMISSIONS = "X-User-Permissions";

    private final SecretKey secretKey;
    private final List<String> whitelist;
    private final String internalSecret;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret,
                         @Value("${jwt.whitelist:/api/auth/login}") String whitelist,
                         @Value("${qws.security.secret:qws-platform-internal-signing-key-2026}") String internalSecret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.whitelist = Arrays.stream(whitelist.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        this.internalSecret = internalSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "未登录或登录已过期");
        }

        String token = authorization.substring(7).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String uid = userId(claims);
            String uname = claims.getSubject();
            String roles = join(claims.get("roles"));
            String perms = join(claims.get("permissions"));
            String signature = InternalSignature.compute(internalSecret, uid, uname, roles, perms);

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .headers(headers -> {
                        // 先清除客户端伪造的同名头，再写入可信值
                        headers.remove(HEADER_USER_ID);
                        headers.remove(HEADER_USERNAME);
                        headers.remove(HEADER_ROLES);
                        headers.remove(HEADER_PERMISSIONS);
                        headers.remove(InternalSignature.HEADER);
                        put(headers, HEADER_USER_ID, uid);
                        put(headers, HEADER_USERNAME, uname);
                        put(headers, HEADER_ROLES, roles);
                        put(headers, HEADER_PERMISSIONS, perms);
                        put(headers, InternalSignature.HEADER, signature);
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT 校验失败 path={} reason={}", path, ex.getMessage());
            return unauthorized(exchange, "登录凭证无效，请重新登录");
        }
    }

    private static String userId(Claims claims) {
        Object id = claims.get("userId");
        return id == null ? claims.getSubject() : String.valueOf(id);
    }

    private static void put(HttpHeaders headers, String name, String value) {
        if (StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static String join(Object claim) {
        if (claim instanceof List<?> list) {
            return String.join(",", ((List<Object>) list).stream().map(String::valueOf).toList());
        }
        return claim == null ? null : String.valueOf(claim);
    }

    private boolean isWhitelisted(String path) {
        // 注意：Spring Cloud Gateway 的 StripPrefix 是在路由阶段生效的，GlobalFilter 看到的是
        // 剥离前的原始路径。两种形态都要兼容，避免白名单形同虚设。
        for (String candidate : new String[]{path, stripApiPrefix(path)}) {
            if (candidate == null) continue;
            if (MATCHER.match("/auth/login", candidate) || MATCHER.match("/auth/profile", candidate)) {
                return true;
            }
            if (whitelist.stream().anyMatch(pattern -> MATCHER.match(pattern, candidate))) {
                return true;
            }
        }
        return false;
    }

    private static String stripApiPrefix(String path) {
        return path != null && path.startsWith("/api/") ? path.substring(4) : null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
