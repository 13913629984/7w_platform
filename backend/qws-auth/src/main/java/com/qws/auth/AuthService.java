package com.qws.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qws.common.entity.SysRolePermission;
import com.qws.common.entity.SysUser;
import com.qws.common.entity.SysUserRole;
import com.qws.common.mapper.SysRolePermissionMapper;
import com.qws.common.mapper.SysUserMapper;
import com.qws.common.mapper.SysUserRoleMapper;
import com.qws.common.util.PasswordUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SecretKey secretKey;

    public AuthService(
            SysUserMapper sysUserMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysRolePermissionMapper sysRolePermissionMapper,
            @Value("${jwt.secret}") String secret) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, Object> login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new AuthException("User not found");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AuthException("User disabled");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new AuthException("Password incorrect");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLogin(LocalDateTime.now());
        sysUserMapper.updateById(update);

        List<String> roles = roleCodes(user.getId());
        List<String> permissions = permissionCodes(roles);

        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("roles", roles)
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
            throw new AuthException("Not logged in");
        }
        String token = authorization.substring(7);
        Claims claims = parseToken(token);
        String username = claims.getSubject();

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new AuthException("User not found");
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

    private List<String> roleCodes(Long userId) {
        List<String> roles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        ).stream().map(SysUserRole::getRoleCode).distinct().toList();
        return roles.isEmpty() ? List.of("SUPER_ADMIN") : roles;
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
        return permissions.isEmpty() ? allSubsystemPermissions() : permissions;
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
            throw new AuthException("Invalid token");
        }
    }
}
