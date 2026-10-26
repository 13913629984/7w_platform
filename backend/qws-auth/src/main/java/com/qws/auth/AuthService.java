package com.qws.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.SysRole;
import com.qws.common.entity.SysRolePermission;
import com.qws.common.entity.SysUser;
import com.qws.common.entity.SysUserRole;
import com.qws.common.mapper.SysRoleMapper;
import com.qws.common.mapper.SysRolePermissionMapper;
import com.qws.common.mapper.SysUserMapper;
import com.qws.common.mapper.SysUserRoleMapper;
import com.qws.common.security.PermissionCodes;
import com.qws.auth.lockout.LoginAttemptStore;
import com.qws.common.util.PasswordUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    /** 历史数据用中文角色名（如「普通用户」），拆不出来就只能给最小权限角色 */
    private static final String DEFAULT_ROLE_CODE = "STAFF";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final LoginAttemptStore attemptStore;
    private final SecretKey secretKey;

    public AuthService(
            SysUserMapper sysUserMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysRolePermissionMapper sysRolePermissionMapper,
            SysRoleMapper sysRoleMapper,
            LoginAttemptStore attemptStore,
            @Value("${jwt.secret}") String secret) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.attemptStore = attemptStore;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 登录失败锁定：默认 5 次 / 15 分钟，可通过配置调整 */
    @Value("${qws.auth.login-lock.max-failures:5}")
    private int maxLoginFailures;

    @Value("${qws.auth.login-lock.lock-minutes:15}")
    private long lockMinutes;

    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new AuthException("请输入用户名和密码");
        }
        String account = username.trim();
        assertNotLocked(account);

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, account)
        );
        if (user == null) {
            // 用户不存在也计数，避免通过响应时间/提示差异枚举账号
            recordFailure(account);
            throw new AuthException("用户名或密码不正确");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AuthException("账号已停用，请联系管理员");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            recordFailure(account);
            throw new AuthException("用户名或密码不正确");
        }

        clearFailures(account);

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLogin(LocalDateTime.now());
        // 渐进式升级：旧的无盐 SHA-256 密码在本次登录成功后立刻重写成 BCrypt。
        // 用 updateById 只改 password，不覆盖其他字段。
        if (PasswordUtil.needsUpgrade(user.getPassword())) {
            update.setPassword(PasswordUtil.hash(password));
            log.info("用户 {} 的密码哈希已从旧 SHA-256 升级为 BCrypt", account);
        }
        sysUserMapper.updateById(update);

        List<String> roles = roleCodes(user.getId());
        List<String> permissions = permissionCodes(roles);

        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(7200)))
                .signWith(secretKey)
                .compact();

        return Map.of(
                "accessToken", token,
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
                "roles", roles,
                "permissions", permissions
        );
    }

    public Map<String, Object> profile(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthException("未登录，请先登录");
        }
        String token = authorization.substring(7);
        Claims claims = parseToken(token);
        String username = claims.getSubject();

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new AuthException("用户不存在或已被删除");
        }

        List<String> roles = roleCodes(user.getId());
        List<String> permissions = permissionCodes(roles);

        return Map.of(
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
                "roles", roles,
                "permissions", permissions
        );
    }

    /**
     * 解析用户的角色码。
     *
     * <p>三个来源，优先级从高到低：
     * <ol>
     *   <li>{@code sys_user_role} 关联表（新模型）；</li>
     *   <li>{@code sys_user.role} 历史字段：可能存角色码（{@code CRM_SALES}），
     *       也可能存中文角色名（{@code 普通用户}），后者需要按 {@code sys_role.name} 反查；</li>
     *   <li>都解析不出来时给 {@link #DEFAULT_ROLE_CODE}（最小权限），绝不回退成超管。</li>
     * </ol>
     *
     * <p>关键是<b>过滤掉不合法的角色码</b>：中文角色名如果原样写进 JWT、再写进 HTTP header，
     * 会直接撞上 Tomcat 的 header ASCII 限制 + 内部签名校验失败，表现为莫名其妙的 401。
     */
    private List<String> roleCodes(Long userId) {
        List<String> fromRelation = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        ).stream().map(SysUserRole::getRoleCode)
                .filter(PermissionCodes::isWellFormedRoleCode)
                .distinct().toList();
        if (!fromRelation.isEmpty()) {
            return fromRelation;
        }

        // 回退到 sys_user.role（历史数据逗号分隔），中文名按角色表反查
        SysUser user = sysUserMapper.selectById(userId);
        String legacy = user == null ? null : user.getRole();
        if (legacy == null || legacy.isBlank()) {
            return List.of(DEFAULT_ROLE_CODE);
        }
        List<String> resolved = java.util.Arrays.stream(legacy.split("[,，]"))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(this::resolveRoleCode)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (resolved.isEmpty()) {
            log.warn("用户 {} 的历史角色字段无法解析为合法角色码：{}，已降级为 {}", userId, legacy, DEFAULT_ROLE_CODE);
            return List.of(DEFAULT_ROLE_CODE);
        }
        return resolved;
    }

    /** 把「角色码 / 中文角色名」统一解析成合法角色码；无法解析返回 null */
    private String resolveRoleCode(String raw) {
        if (PermissionCodes.isWellFormedRoleCode(raw)) {
            return raw;
        }
        // 中文名 -> code
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getName, raw).last("LIMIT 1"));
        if (role != null && PermissionCodes.isWellFormedRoleCode(role.getCode())) {
            log.debug("历史角色名 {} 解析为角色码 {}", raw, role.getCode());
            return role.getCode();
        }
        return null;
    }

    // ---------------- 登录失败锁定 ----------------

    private void assertNotLocked(String account) {
        Duration remain = attemptStore.lockedFor(account);
        if (remain.isZero() || remain.isNegative()) {
            return;
        }
        // 向上取整到分钟，避免显示「请 0 分钟后再试」
        long minutes = Math.max(1, (remain.getSeconds() + 59) / 60);
        throw new AuthException("登录失败次数过多，账号已锁定，请 " + minutes + " 分钟后再试", 423);
    }

    private void recordFailure(String account) {
        attemptStore.recordFailure(account, maxLoginFailures, Duration.ofMinutes(lockMinutes));
    }

    private void clearFailures(String account) {
        attemptStore.clear(account);
    }

    private List<String> permissionCodes(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        if (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN")) {
            return allSubsystemPermissions();
        }
        List<String> permissions = sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleCode, roles)
        ).stream()
                .map(SysRolePermission::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .collect(Collectors.toList());
        // 角色没有配置任何权限时返回空集合（默认拒绝），
        // 不能回退成“全部子系统权限”，否则配错角色会静默变成全权限。
        return permissions;
    }

    private List<String> allSubsystemPermissions() {
        return List.of("SYS", "CRM", "WMS", "MAINT", "ETS", "FIN");
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new AuthException("登录凭证无效，请重新登录");
        }
    }
}
