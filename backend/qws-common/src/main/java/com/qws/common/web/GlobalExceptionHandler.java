package com.qws.common.web;

import com.qws.common.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理：把所有框架层异常收敛成统一的 {@link ApiResult} 结构。
 *
 * <p>为什么需要它：
 * <ul>
 *   <li>没有它时，缺参数 / 类型不匹配 / JSON 格式错 会走 Spring 默认错误页，
 *       返回 {@code {timestamp,status,error,path}}，<b>没有 code 字段</b>，
 *       前端 {@code res.code !== 0} 判断会拿到 undefined，只能弹「接口请求失败」；</li>
 *   <li>业务异常走 {@code ApiResult.fail()} 返回 HTTP 200 + code 500，
 *       框架异常却返回 HTTP 400 —— 前端要写两套判断逻辑。</li>
 * </ul>
 *
 * <p>统一后有两类失败，各自职责清晰：
 * <ul>
 *   <li><b>业务失败</b>（参数不合法 / 状态冲突）：HTTP 200 + {@code code=422/409} + 中文 message。
 *       保持历史上的「HTTP 总是 200」契约，前端已有的 {@code res.code !== 0} 判断全部继续有效；</li>
 *   <li><b>框架失败</b>（缺参 / 类型错 / JSON 解析失败 / 未预期异常）：HTTP 4xx/5xx
 *       + 同样结构的 {@code ApiResult}（含 code 与中文 message），
 *       前端 axios 会 reject，拦截器把 message 转成可读文案。</li>
 * </ul>
 *
 * <p>关键收益：框架错误不再返回 {@code {timestamp,status,error,path}} 这种没有 code 的体，
 * 前端不会再把「缺参数」误判成「接口请求失败」。
 *
 * <p>各服务主类都是 {@code scanBasePackages = "com.qws"}，因此这个类放在 qws-common
 * 会自动对所有微服务生效，不需要逐个服务配置。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务校验失败（参数不合法）：保持 HTTP 200 + code 422，不改变前端既有处理方式 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.debug("参数校验失败 path={} message={}", request.getRequestURI(), ex.getMessage());
        return new ApiResult<>(422, message(ex, "请求参数不合法"), null);
    }

    /** 业务状态冲突（如「订单已生成出库单」「库存不足」）：HTTP 200 + code 409 */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.debug("业务状态冲突 path={} message={}", request.getRequestURI(), ex.getMessage());
        return new ApiResult<>(409, message(ex, "当前状态不允许该操作"), null);
    }

    /** 缺必填 query 参数 -> 400，message 里点名是哪个参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return body(HttpStatus.BAD_REQUEST, 400,
                "缺少必填参数：" + ex.getParameterName() + "（" + ex.getParameterType() + "）");
    }

    /** query 参数类型不对（如 id 传了 "abc"）-> 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return body(HttpStatus.BAD_REQUEST, 400,
                "参数类型不正确：" + ex.getName() + " = " + ex.getValue());
    }

    /** JSON 体解析失败（字段类型/时间格式不对，或请求不是合法 JSON）-> 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleNotReadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        String detail = rootMessage(ex);
        log.warn("请求体解析失败 path={} detail={}", request.getRequestURI(), detail);
        return body(HttpStatus.BAD_REQUEST, 400, "请求体格式不正确：" + detail);
    }

    /** @Valid 校验失败 -> 400，把字段错误拼成一句话 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResult<Void>> handleValidation(BindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describeFieldError)
                .collect(Collectors.joining("；"));
        return body(HttpStatus.BAD_REQUEST, 400, detail.isBlank() ? "请求参数校验失败" : detail);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, 405,
                "不支持的请求方法：" + ex.getMethod() + "，请使用 " + String.join("/", ex.getSupportedMethods() == null ? new String[0] : ex.getSupportedMethods()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNoHandler(NoHandlerFoundException ex) {
        return body(HttpStatus.NOT_FOUND, 404, "接口不存在：" + ex.getRequestURL());
    }

    /** 兜底：未预期异常 -> 500，日志留全栈但不把堆栈暴露给前端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("未处理异常 path={} ", request.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, 500,
                "服务内部错误，请联系管理员（" + ex.getClass().getSimpleName() + "）");
    }

    private ResponseEntity<ApiResult<Void>> body(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(new ApiResult<>(code, message, null));
    }

    private String message(Throwable ex, String fallback) {
        String text = ex.getMessage();
        return text == null || text.isBlank() ? fallback : text;
    }

    private String describeFieldError(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        return error.getField() + " " + (defaultMessage == null ? "不合法" : defaultMessage);
    }

    /** 取最内层异常的消息（Spring 会层层包装，最外层信息量最小） */
    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String text = current.getMessage();
        if (text == null || text.isBlank()) {
            return current.getClass().getSimpleName();
        }
        // Jackson 的报错很长，截断到可读长度
        return text.length() > 240 ? text.substring(0, 240) + "..." : text;
    }
}
