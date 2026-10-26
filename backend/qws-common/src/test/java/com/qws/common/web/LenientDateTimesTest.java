package com.qws.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 时间格式回归测试。
 *
 * <p>对应第 2 批修掉的问题：前端存在两套 value-format，
 * Jackson 默认只认 ISO，导致改一处格式就静默 400。
 */
class LenientDateTimesTest {

    @Test
    @DisplayName("四种写法都能解析成同一个时间")
    void parseAllCommonFormats() {
        LocalDateTime expected = LocalDateTime.of(2026, 10, 26, 11, 0, 0);
        assertEquals(expected, LenientDateTimes.parseDateTime("2026-10-26T11:00:00"), "ISO 带 T 带秒");
        assertEquals(expected, LenientDateTimes.parseDateTime("2026-10-26 11:00:00"), "空格带秒");
        assertEquals(expected, LenientDateTimes.parseDateTime("2026-10-26 11:00"), "空格无秒");
        assertEquals(expected, LenientDateTimes.parseDateTime("2026-10-26T11:00"), "T 无秒");
    }

    @Test
    @DisplayName("纯日期按当天零点处理")
    void parseDateOnly() {
        assertEquals(LocalDateTime.of(2026, 10, 26, 0, 0, 0),
                LenientDateTimes.parseDateTime("2026-10-26"));
    }

    @Test
    @DisplayName("空值返回 null，不抛异常")
    void nullAndBlank() {
        assertNull(LenientDateTimes.parseDateTime(null));
        assertNull(LenientDateTimes.parseDateTime(""));
        assertNull(LenientDateTimes.parseDateTime("   "));
        assertNull(LenientDateTimes.parseDate(null));
    }

    @Test
    @DisplayName("非法格式抛带原文的中文异常（不是 JSON parse error）")
    void invalidFormatHasReadableMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LenientDateTimes.parseDateTime("26/10/2026"));
        assertTrue(ex.getMessage().contains("26/10/2026"), "错误信息应包含原始输入");
        assertTrue(ex.getMessage().contains("时间格式不正确"));
    }

    @Test
    @DisplayName("LocalDate 解析：纯日期 / 带时间 / 斜杠")
    void parseDate() {
        assertEquals(LocalDate.of(2026, 10, 26), LenientDateTimes.parseDate("2026-10-26"));
        assertEquals(LocalDate.of(2026, 10, 26), LenientDateTimes.parseDate("2026/10/26"));
        // 带时间的字符串取日期部分
        assertEquals(LocalDate.of(2026, 10, 26), LenientDateTimes.parseDate("2026-10-26 11:00:00"));
    }

    @Test
    @DisplayName("输出统一 yyyy-MM-dd HH:mm:ss（无 T）")
    void formatOutput() {
        LocalDateTime value = LocalDateTime.of(2026, 10, 26, 11, 5, 9);
        assertEquals("2026-10-26 11:05:09", LenientDateTimes.format(value));
        assertEquals("2026-10-26", LenientDateTimes.format(LocalDate.of(2026, 10, 26)));
        assertNull(LenientDateTimes.format((LocalDateTime) null));
    }
}
