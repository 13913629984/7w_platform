package com.qws.common.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qws.common.ApiResult;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 统一错误端点：把 Spring 默认的 Whitelabel / 错误页收敛成 {@link ApiResult}。
 *
 * <p>为什么需要它：
 * <ul>
 *   <li>默认的 {@code /error} 返回 {@code {timestamp,status,error,path}}，<b>没有 code 字段</b>，
 *       前端 {@code res.code !== 0} 会拿到 undefined，只能提示"接口请求失败"；</li>
 *   <li>路径不存在时（如 {@code /api/wms/不存在的路径}）应当明确返回 404 + 中文说明，
 *       而不是一坨 Spring 默认体。</li>
 * </ul>
 *
 * <p>注意：本类不处理业务异常（那些由 {@link GlobalExceptionHandler} 负责），
 * 只负责容器级错误（404 / 容器抛出的 500 等）。
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResult<Void>> handleError(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute("jakarta.servlet.error.status_code");
        int status = statusAttribute instanceof Integer value ? value : HttpStatus.INTERNAL_SERVER_ERROR.value();

        Object pathAttribute = request.getAttribute("jakarta.servlet.error.request_uri");
        String path = pathAttribute == null ? "" : String.valueOf(pathAttribute);

        String message = switch (status) {
            case 404 -> "接口不存在：" + path;
            case 405 -> "不支持的请求方法";
            case 401 -> "未登录或登录已过期";
            case 403 -> "没有访问权限";
            default -> status >= 500 ? "服务内部错误，请联系管理员" : "请求失败（HTTP " + status + "）";
        };
        return ResponseEntity.status(status).body(new ApiResult<>(status, message, null));
    }
}
