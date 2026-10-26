package com.qws.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qws.common.ApiResult;
import com.qws.common.entity.SysAuditLog;
import com.qws.common.mapper.SysAuditLogMapper;
import com.qws.common.security.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * 审计日志切面：拦截 {@link Audit} 注解，自动写 {@code sys_audit_log}。
 *
 * <p>为什么放在 qws-common：所有微服务都是 {@code scanBasePackages = "com.qws"}，
 * 因此切面在每个服务里自动生效，写的是同一个库的同一张表 —— 「审计日志」页面
 * 不需要跨服务聚合就能看到全平台操作。
 *
 * <p>几个关键决定：
 * <ul>
 *   <li><b>失败也记</b>：业务抛异常或返回 code != 0 时记 {@code status=fail}，
 *       并带上错误信息。审计的价值一半在"谁试图做了什么失败的事"；</li>
 *   <li><b>审计失败不影响业务</b>：写日志本身出错只打 warn，绝不让业务回滚；</li>
 *   <li><b>敏感字段脱敏</b>：payload 里的 password / token 一律替换成 {@code ***}；</li>
 *   <li><b>截断超长内容</b>：{@code content} 列 500 字符、{@code detail} 是 TEXT，
 *       请求体可能很大，必须裁剪，否则插入会失败。</li>
 * </ul>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final int CONTENT_MAX = 480;
    private static final int DETAIL_MAX = 4000;
    private static final String[] SENSITIVE_KEYS = {"password", "passwd", "pwd", "token", "secret", "accessToken"};

    private final SysAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditAspect(SysAuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            try {
                record(joinPoint, audit, result, failure, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                // 审计写失败不能影响主流程
                log.warn("写审计日志失败 method={} error={}", joinPoint.getSignature().toShortString(), ex.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint joinPoint, Audit audit, Object result, Throwable failure, long costMs) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // ---- 成功/失败判定 ----
        String status = "success";
        String errorMessage = null;
        if (failure != null) {
            status = "fail";
            errorMessage = failure.getMessage();
        } else if (result instanceof ApiResult<?> apiResult && apiResult.code() != 0) {
            status = "fail";
            errorMessage = apiResult.message();
        }

        String username = username(audit, joinPoint);
        String actionLabel = audit.action().getLabel();
        String moduleName = audit.moduleName().isBlank() ? defaultModuleName(audit.module()) : audit.moduleName();

        SysAuditLog entity = new SysAuditLog();
        entity.setOperateTime(LocalDateTime.now());
        entity.setUsername(username);
        entity.setModuleCode(audit.module());
        entity.setModuleName(moduleName);
        entity.setActionType(audit.action().name());
        entity.setActionName(actionLabel);
        entity.setContent(truncate(username + " " + actionLabel + " " + audit.target(), CONTENT_MAX));
        entity.setTargetName(truncate(audit.target(), 100));
        entity.setStatus(status);
        entity.setCostMs((int) Math.min(costMs, Integer.MAX_VALUE));
        entity.setCreateTime(LocalDateTime.now());

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setIpAddress(clientIp(request));
            entity.setBrowser(truncate(request.getHeader("User-Agent"), 120));
            entity.setRequestId(request.getHeader("X-Request-Id"));
        }
        if (entity.getRequestId() == null || entity.getRequestId().isBlank()) {
            entity.setRequestId(UUID.randomUUID().toString());
        }
        // 接口名放在 detail 里，便于定位是哪个接口出了问题
        String handler = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        StringBuilder detail = new StringBuilder("接口: ").append(handler);
        if (audit.recordPayload()) {
            String payload = payload(joinPoint.getArgs());
            if (!payload.isBlank()) {
                detail.append("\n参数: ").append(payload);
            }
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            detail.append("\n错误: ").append(errorMessage);
        }
        entity.setDetail(truncate(detail.toString(), DETAIL_MAX));

        auditLogMapper.insert(entity);
        log.debug("审计已记录 module={} action={} user={} status={}", audit.module(), audit.action(), username, status);
    }

    /**
     * 解析操作人。
     *
     * <p>优先取登录上下文（{@link UserContext}）；为空时（典型场景：登录接口本身、
     * 或者 token 已失效）再尝试从 {@link Audit#usernameKey()} 指定的请求字段里取。
     * 这样「admin 尝试登录失败」也能被记录，而不是统统变成 "system"。
     */
    private String username(Audit audit, ProceedingJoinPoint joinPoint) {
        String fromContext = UserContext.currentUsername();
        if (fromContext != null && !fromContext.isBlank()) {
            return fromContext;
        }
        if (!audit.usernameKey().isBlank()) {
            String fromPayload = readField(joinPoint.getArgs(), audit.usernameKey());
            if (fromPayload != null && !fromPayload.isBlank()) {
                return truncate(fromPayload, 50);
            }
        }
        return "system";
    }

    /** 从参数对象（或其 record 访问器）里读字符串字段 */
    private String readField(Object[] args, String fieldName) {
        if (args == null) {
            return null;
        }
        String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (Object arg : args) {
            if (arg == null) continue;
            // record 的访问器名就是字段名
            for (String candidate : new String[]{fieldName, getter}) {
                try {
                    var method = arg.getClass().getMethod(candidate);
                    Object value = method.invoke(arg);
                    if (value != null) {
                        return String.valueOf(value);
                    }
                } catch (Exception ignored) {
                    // 试下一个
                }
            }
        }
        return null;
    }

    /** 请求参数序列化 + 脱敏 */
    private String payload(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        Object[] serializable = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                .toArray();
        if (serializable.length == 0) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(serializable.length == 1 ? serializable[0] : serializable);
            return maskSensitive(json);
        } catch (Exception ex) {
            return "";
        }
    }

    /** 把 password/token 这类字段的值替换成 *** */
    private String maskSensitive(String json) {
        String masked = json;
        for (String key : SENSITIVE_KEYS) {
            // "key":"value"  /  "key": "value"
            masked = masked.replaceAll("(?i)(\"" + key + "\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
        }
        return masked;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /** 取真实客户端 IP（网关转发后取 X-Forwarded-For 的第一个） */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private String defaultModuleName(String moduleCode) {
        return switch (moduleCode) {
            case "SYS" -> "系统管理";
            case "CRM" -> "CRM客户管理";
            case "WMS" -> "WMS仓储管理";
            case "MAINT" -> "维保系统";
            case "ETS" -> "电测系统";
            case "FIN" -> "财务系统";
            default -> moduleCode;
        };
    }
}
