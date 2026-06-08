package com.xxxlog.client.config;

import com.xxxlog.common.enums.TraceMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 2.x / javax.servlet 自动配置。
 * <p>
 * 通过反射注册 Filter，避免与 Boot 3 的 {@code jakarta.servlet.Filter} 产生编译期类型冲突。
 */
@Configuration
@ConditionalOnExpression("${xxx-log.trace-enabled:true} == true and '${xxx-log.trace-mode:native}' != 'micrometer'")
@ConditionalOnClass(name = "javax.servlet.Filter")
@ConditionalOnMissingClass("jakarta.servlet.Filter")
@EnableConfigurationProperties(XxxLogProperties.class)
public class XxxLogJavaxAutoConfiguration {

    @Bean
    public ServletContextInitializer xxxLogJavaxTraceFilterInitializer(XxxLogProperties properties) {
        final TraceMode traceMode = XxxLogAutoConfiguration.resolveMode(properties);
        return servletContext -> registerJavaxTraceFilter(servletContext, traceMode);
    }

    private static void registerJavaxTraceFilter(Object servletContext, TraceMode traceMode) {
        try {
            Class<?> filterClass = Class.forName("com.xxxlog.client.trace.TraceFilter");
            Class<?> javaxFilterClass = Class.forName("javax.servlet.Filter");
            Object filter = filterClass.getConstructor(TraceMode.class).newInstance(traceMode);

            Object registration = servletContext.getClass()
                    .getMethod("addFilter", String.class, javaxFilterClass)
                    .invoke(servletContext, "xxxLogTraceFilter", filter);
            if (registration != null) {
                registration.getClass()
                        .getMethod("addMappingForUrlPatterns", String[].class)
                        .invoke(registration, (Object) new String[]{"/*"});
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register javax TraceFilter", e);
        }
    }
}
