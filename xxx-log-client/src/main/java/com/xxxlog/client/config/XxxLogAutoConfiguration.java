package com.xxxlog.client.config;

import com.xxxlog.client.trace.JakartaTraceFilter;
import com.xxxlog.common.enums.TraceMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 3.x / jakarta.servlet 自动配置。
 */
@Configuration
@ConditionalOnExpression("${xxx-log.trace-enabled:true} == true and '${xxx-log.trace-mode:native}' != 'micrometer'")
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@EnableConfigurationProperties(XxxLogProperties.class)
public class XxxLogAutoConfiguration {

    @Bean
    public FilterRegistrationBean<JakartaTraceFilter> xxxLogJakartaTraceFilter(XxxLogProperties properties) {
        FilterRegistrationBean<JakartaTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JakartaTraceFilter(resolveMode(properties)));
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        registration.setName("xxxLogTraceFilter");
        return registration;
    }

    static TraceMode resolveMode(XxxLogProperties properties) {
        TraceMode mode = properties.getTraceMode();
        return mode == null ? TraceMode.NATIVE : mode;
    }
}
