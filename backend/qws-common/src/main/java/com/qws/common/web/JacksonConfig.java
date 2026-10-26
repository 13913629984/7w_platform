package com.qws.common.web;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 全局 Jackson 时间格式配置。
 *
 * <p>统一 {@link LocalDateTime} / {@link LocalDate} 的读写格式：
 * <ul>
 *   <li>写：{@code yyyy-MM-dd HH:mm:ss} / {@code yyyy-MM-dd}（前端表格直接显示，不需要再 replace('T',' ')）</li>
 *   <li>读：宽松接受 ISO（带 T）、空格分隔、省略秒、纯日期（见 {@link LenientDateTimes}）</li>
 * </ul>
 *
 * <p>各微服务主类都是 {@code scanBasePackages = "com.qws"}，本类放 qws-common 自动生效。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer lenientDateTimeCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("qws-lenient-datetime");
            module.addDeserializer(LocalDateTime.class, new LenientDateTimes.LocalDateTimeDeserializer());
            module.addSerializer(LocalDateTime.class, new LenientDateTimes.LocalDateTimeSerializer());
            module.addDeserializer(LocalDate.class, new LenientDateTimes.LocalDateDeserializer());
            module.addSerializer(LocalDate.class, new LenientDateTimes.LocalDateSerializer());
            builder.modules(module);
        };
    }
}
