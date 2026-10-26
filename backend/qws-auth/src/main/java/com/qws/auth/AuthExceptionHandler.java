package com.qws.auth;

import com.qws.common.ApiResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证异常 -> 真实 HTTP 状态码。
 *
 * <p>关键点：{@code /auth/profile} 是网关白名单接口，网关不校验 token，
 * 由本服务自己解析。返回 HTTP 200 会让前端「以为拿到 profile」，
 * 用空数据覆盖本地权限；返回 401 才能让前端正常走「清理凭证 + 回登录页」流程。
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthException(AuthException ex) {
        int status = ex.getStatus();
        return ResponseEntity.status(status)
                .body(new ApiResult<>(status, ex.getMessage(), null));
    }
}
