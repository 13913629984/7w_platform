package com.qws.auth;

import com.qws.common.ApiResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ApiResult<Void> handleAuthException(AuthException ex) {
        return ApiResult.fail(ex.getMessage());
    }
}
