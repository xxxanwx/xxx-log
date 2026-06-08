package com.xxxlog.client.cloud;

import com.xxxlog.client.config.XxxLogProperties;
import com.xxxlog.common.enums.TraceMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring Cloud 微服务链路透传自动配置。
 * <p>
 * 需配合 xxx-log-client，并设置 {@code xxx-log.trace-mode=compatible}。
 */
@Configuration
@ConditionalOnProperty(prefix = "xxx-log", name = "trace-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "xxx-log", name = "trace-mode", havingValue = "micrometer", matchIfMissing = false, negate = true)
@EnableConfigurationProperties(XxxLogProperties.class)
public class XxxLogCloudAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    public FeignTraceInterceptor xxxLogFeignTraceInterceptor(XxxLogProperties properties) {
        return new FeignTraceInterceptor(resolveMode(properties));
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
    public RestTemplateCustomizer xxxLogRestTemplateCustomizer(XxxLogProperties properties) {
        final TraceRestTemplateInterceptor interceptor = new TraceRestTemplateInterceptor(resolveMode(properties));
        return new RestTemplateCustomizer() {
            @Override
            public void customize(org.springframework.web.client.RestTemplate restTemplate) {
                restTemplate.getInterceptors().add(interceptor);
            }
        };
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    public WebClientCustomizer xxxLogWebClientCustomizer(XxxLogProperties properties) {
        final WebClientTraceFilter filter = new WebClientTraceFilter(resolveMode(properties));
        return new WebClientCustomizer() {
            @Override
            public void customize(WebClient.Builder builder) {
                builder.filter(filter);
            }
        };
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public GatewayTraceGlobalFilter xxxLogGatewayTraceGlobalFilter(XxxLogProperties properties) {
        return new GatewayTraceGlobalFilter(properties);
    }

    @Bean
    public AsyncTraceTaskDecorator xxxLogAsyncTraceTaskDecorator() {
        return new AsyncTraceTaskDecorator();
    }

    @Bean
    public static BeanPostProcessor xxxLogAsyncExecutorPostProcessor(AsyncTraceTaskDecorator decorator) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof ThreadPoolTaskExecutor) {
                    ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
                    if (executor.getTaskDecorator() == null) {
                        executor.setTaskDecorator(decorator);
                    }
                }
                return bean;
            }
        };
    }

    private static TraceMode resolveMode(XxxLogProperties properties) {
        TraceMode mode = properties.getTraceMode();
        return mode == null ? TraceMode.COMPATIBLE : mode;
    }
}
