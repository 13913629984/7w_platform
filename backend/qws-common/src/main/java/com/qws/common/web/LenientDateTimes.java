package com.qws.common.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 时间字段的宽松序列化/反序列化。
 *
 * <p>历史上平台里同时存在两套时间约定：
 * <ul>
 *   <li>前端 {@code el-date-picker} 的 {@code value-format="YYYY-MM-DD HH:mm"} → 传 {@code 2026-10-26 11:00}；</li>
 *   <li>另一处用 {@code value-format="YYYY-MM-DD[T]HH:mm:ss"} → 传 {@code 2026-10-26T11:00:00}。</li>
 * </ul>
 * 而 Jackson 默认只认 ISO-8601（{@code T} 分隔），所以只要有人改了某处的 value-format，
 * 就会静默变成 400「JSON parse error」，排查很痛苦。
 *
 * <p>这里统一接受所有常见写法，输出统一成 {@code yyyy-MM-dd HH:mm:ss}：
 * <ul>
 *   <li>{@code 2026-10-26T11:00:00}（ISO）</li>
 *   <li>{@code 2026-10-26 11:00:00}</li>
 *   <li>{@code 2026-10-26 11:00}</li>
 *   <li>{@code 2026-10-26}（按当天 00:00:00 处理）</li>
 * </ul>
 */
public final class LenientDateTimes {

    private static final DateTimeFormatter OUT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<DateTimeFormatter> DATE_TIME_INPUTS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,                                  // 2026-10-26T11:00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),                    // 2026-10-26 11:00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),                       // 2026-10-26 11:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),                     // 2026-10-26T11:00
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    );

    private static final List<DateTimeFormatter> DATE_INPUTS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                                      // 2026-10-26
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    private LenientDateTimes() {
    }

    /**
     * 解析 LocalDateTime；空串/空值返回 null，格式不认识抛带原文的异常（便于前端定位）。
     *
     * <p>注意<b>不能按字符串长度分支</b>：{@code 26/10/2026} 也是 10 个字符，
     * 按长度判断会把它当日期去解析，报出"日期格式不正确"，掩盖真正的问题。
     * 改为：先按 datetime 格式逐个试，都不中再按日期格式试（纯日期 -> 当天零点）。
     */
    public static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        for (DateTimeFormatter formatter : DATE_TIME_INPUTS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种
            }
        }
        // 最后尝试当作纯日期
        for (DateTimeFormatter formatter : DATE_INPUTS) {
            try {
                return LocalDate.parse(text, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种
            }
        }
        throw new IllegalArgumentException("时间格式不正确：" + raw
                + "（支持 yyyy-MM-dd、yyyy-MM-dd HH:mm[:ss]、yyyy-MM-ddTHH:mm[:ss]）");
    }

    /**
     * 解析 LocalDate；空值返回 null。
     *
     * <p>同样不按长度分支：先按日期格式试，不中再交给 {@link #parseDateTime} 取日期部分
     * （前端偶尔会直接传 datetime 字符串）。
     */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        for (DateTimeFormatter formatter : DATE_INPUTS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种
            }
        }
        // 退一步：当作完整时间解析，再取日期部分
        for (DateTimeFormatter formatter : DATE_TIME_INPUTS) {
            try {
                return LocalDateTime.parse(text, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种
            }
        }
        throw new IllegalArgumentException("日期格式不正确：" + raw + "（支持 yyyy-MM-dd）");
    }

    /** 解析时间（HH:mm[:ss]） **/
    public static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        try {
            return text.length() <= 5
                    ? LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm"))
                    : LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("时间格式不正确：" + raw + "（支持 HH:mm[:ss]）");
        }
    }

    /** LocalDateTime -> 字符串（统一 yyyy-MM-dd HH:mm:ss） */
    public static String format(LocalDateTime value) {
        return value == null ? null : value.format(OUT_DATE_TIME);
    }

    /** LocalDate -> 字符串（yyyy-MM-dd） */
    public static String format(LocalDate value) {
        return value == null ? null : value.format(OUT_DATE);
    }

    // ---------------- Jackson 适配器 ----------------

    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parseDateTime(parser.getValueAsString());
        }
    }

    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeString(format(value));
        }
    }

    public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parseDate(parser.getValueAsString());
        }
    }

    public static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        @Override
        public void serialize(LocalDate value, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeString(format(value));
        }
    }
}
