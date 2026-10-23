package com.qws.ets;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** 电测模块通用工具：日期解析、分页规整、数值计算 */
final class EtsSupport {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private EtsSupport() {
    }

    static int normalizePage(int page) {
        return page <= 0 ? 1 : page;
    }

    static int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 500);
    }

    /** 解析 yyyy-MM-dd 日期，空串返回 null */
    static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        if (text.length() > 10) {
            text = text.substring(0, 10);
        }
        try {
            return LocalDate.parse(text, DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    /** 解析 yyyy-MM-dd HH:mm 时间，也兼容带秒的格式，空串返回 null */
    static LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim().replace('T', ' ');
        try {
            if (text.length() == 10) {
                return LocalDate.parse(text, DATE).atStartOfDay();
            }
            if (text.length() == 16) {
                return LocalDateTime.parse(text, DATETIME);
            }
            if (text.length() >= 19) {
                return LocalDateTime.parse(text.substring(0, 19).replace(' ', 'T'));
            }
            throw new IllegalArgumentException("时间格式不正确，应为 yyyy-MM-dd HH:mm");
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("时间格式不正确，应为 yyyy-MM-dd HH:mm");
        }
    }

    /** 解析数字，空串返回 null */
    static BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("数值格式不正确");
        }
    }

    /** 百分比计算：分子/分母*100，保留 2 位，分母为 0 时返回 0 */
    static BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /** 生成 前缀+yyyyMMdd+3位序号 的业务编号 */
    static String nextCode(String prefix, long todayCount) {
        return prefix + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", todayCount + 1);
    }
}
