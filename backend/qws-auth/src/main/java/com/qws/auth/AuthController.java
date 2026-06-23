package com.qws.auth;

import com.qws.common.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
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
