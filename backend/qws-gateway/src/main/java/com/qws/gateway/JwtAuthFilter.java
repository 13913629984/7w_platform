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

    // 开发环境默认值；生产环境必须通过环境变量 QWS_INTERNAL_SECRET 覆盖
    public static final String DEFAULT_INTERNAL_SECRET = "${qws.security.secret:${QWS_INTERNAL_SECRET:qws-platform-internal-signing-key-2026}}";

    public JwtAuthFilter(@Value("${jwt.secret}") String secret,
                         @Value("${jwt.whitelist:/api/auth/login}") String whitelist,
                         @Value(DEFAULT_INTERNAL_SECRET) String internalSecret) {
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

            // HTTP header 只能是 ASCII（Tomcat 不允向 header 写非 ASCII，否则直接 400/乱码）。
            // 用户名与角色名可能是中文（历史数据里就有"普通用户"），
            // 统一在网关侧 percent-encode，下游 UserContextInterceptor 再 decode。
            // 签名在「已编码值」上计算，保证两侧 payload 完全一致。
            String uidHeader = encodeHeader(uid);
            String unameHeader = encodeHeader(uname);
            String rolesHeader = encodeHeaderList(roles);
            String permsHeader = encodeHeaderList(perms);
            String signature = InternalSignature.compute(internalSecret, uidHeader, unameHeader, rolesHeader, permsHeader);

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .headers(headers -> {
                        // 先清除客户端伪造的同名头，再写入可信值
                        headers.remove(HEADER_USER_ID);
                        headers.remove(HEADER_USERNAME);
                        headers.remove(HEADER_ROLES);
                        headers.remove(HEADER_PERMISSIONS);
                        headers.remove(InternalSignature.HEADER);
                        put(headers, HEADER_USER_ID, uidHeader);
                        put(headers, HEADER_USERNAME, unameHeader);
                        put(headers, HEADER_ROLES, rolesHeader);
                        put(headers, HEADER_PERMISSIONS, permsHeader);
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

    /**
     * 把可能含非 ASCII 的头值转成纯 ASCII（RFC 3986 percent-encoding）。
     * 纯 ASCII 值保持原样，不做多余转义（否则会让下游的逗号分隔解析变复杂）。
     */
    static String encodeHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        boolean ascii = value.chars().allMatch(c -> c >= 0x20 && c < 0x7F);
        if (ascii) {
            return value;
        }
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 逗号分隔的列表头：逐个元素编码后再拼回逗号。
     * 不能整体编码——整体编码会把分隔符逗号也编成 {@code %2C}，下游就拆不出多个值了。
     */
    static String encodeHeaderList(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(JwtAuthFilter::encodeHeader)
                .collect(java.util.stream.Collectors.joining(","));
    }

    @SuppressWarnings("unchecked")
    private static String join(Object claim) {
        if (claim instanceof List<?> list) {
            return String.join(",", ((List<Object>) list).stream().map(String::valueOf).toList());
        }
        return claim == null ? null : String.valueOf(claim);
    }

    /**
     * 白名单判定。
     *
     * <p><b>关键事实</b>（实测得出，别再改错）：{@code GlobalFilter} 拿到的
     * {@code exchange.getRequest().getURI().getPath()} 是 <b>StripPrefix 已生效</b>后的
     * 路径。例如外部请求 {@code /api/auth/login}，经过 {@code Path=/api/auth/** +
     * StripPrefix=1} 路由后，filter 里看到的是 {@code /auth/login}。
     *
     * <p>因此必须<b>两种形态都匹配</b>：
     * <ul>
     *   <li>剥离后：{@code /auth/login}（StripPrefix 路由）</li>
     *   <li>原始：{@code /api/auth/login}（未配置 StripPrefix 的路由，如 actuator 直通）</li>
     * </ul>
     * 只匹配一种就会漏掉另一类路由，表现为"登录接口莫名 401"。
     */
    private boolean isWhitelisted(String path) {
        if (path == null) {
            return false;
        }
        return whitelist.stream().anyMatch(pattern ->
                MATCHER.match(pattern, path) || MATCHER.match(pattern, withApiPrefix(path)));
    }

    /** 给剥离后的路径补回 /api 前缀，用于比对配置里以 /api 开头的白名单项 */
    private static String withApiPrefix(String path) {
        return path.startsWith("/api/") ? path : "/api" + path;
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
