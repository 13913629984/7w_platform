package com.qws.auth;

import com.qws.common.ApiResult;
import com.qws.common.audit.ActionType;
import com.qws.common.audit.Audit;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录。
     *
     * <p>审计要点：
     * <ul>
     *   <li>{@code recordPayload = false}：请求体里有明文密码，绝不能写进审计表；</li>
     *   <li>{@code usernameKey = "username"}：登录时还没有登录上下文，
     *       靠这个从请求体取操作人，否则日志里全是 "system"；</li>
     *   <li>登录失败（密码错/账号停用）会抛 AuthException，切面仍会记一条 status=fail，
     *       这正是排查「暴力破解 / 账号被锁」最需要的记录。</li>
     * </ul>
     */
    @PostMapping("/login")
    @Audit(module = "SYS", action = ActionType.LOGIN, target = "平台登录",
            recordPayload = false, usernameKey = "username")
    public ApiResult<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request.username(), request.password()));
    }

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResult.ok(authService.profile(authorization));
    }

    public record LoginRequest(String username, String password) {
    }
}
