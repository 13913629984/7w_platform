package com.qws.fin;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务模块通用的类型转换辅助方法，统一处理 Map 请求参数中的字符串与金额解析。
 */
final class FinSupport {

    private FinSupport() {
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static String defaultText(Object value, String defaultValue) {
        String text = text(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    static BigDecimal decimal(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return BigDecimal.ZERO;
        return new BigDecimal(String.valueOf(value));
    }

    static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    static boolean contains(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword);
    }

    /** 规整分页参数，page 最小为 1，pageSize 限定在 1~200 之间。 */
    static long normalizePage(Integer page) {
        return page == null || page < 1 ? 1L : page;
    }

    static long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) return 10L;
        return Math.min(pageSize, 200L);
    }

    /** 统一的分页返回结构：{rows, total, page, pageSize}。 */
    static Map<String, Object> pageResult(List<Map<String, Object>> rows, long total, long page, long pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }
}
