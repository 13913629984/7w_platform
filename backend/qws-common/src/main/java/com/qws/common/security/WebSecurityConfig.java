package com.qws.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 {@link UserContextInterceptor}。
 *
 * <p>各微服务主类都是 {@code @SpringBootApplication(scanBasePackages = "com.qws")}，
 * 因此只要本类带 {@code @Configuration} 就会被自动扫描到，无需逐个服务改主类。
 *
 * <p>配置项：
 * <ul>
 *   <li>{@code qws.security.secret}：与网关共享的内部签名密钥；</li>
 *   <li>{@code qws.security.require-signature}：是否强制校验签名（默认 true）。</li>
 * </ul>
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    private final String secret;
    private final boolean requireSignature;

    public WebSecurityConfig(
            @Value("${qws.security.secret:qws-platform-internal-signing-key-2026}") String secret,
            @Value("${qws.security.require-signature:true}") boolean requireSignature) {
        this.secret = secret;
        this.requireSignature = requireSignature;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor(secret, requireSignature))
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/error");
    }
}
