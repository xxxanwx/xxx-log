package com.xxxlog.demo.config;

import com.xxxlog.demo.interceptor.DemoTraceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DemoWebConfig implements WebMvcConfigurer {

    private final DemoTraceInterceptor demoTraceInterceptor;

    public DemoWebConfig(DemoTraceInterceptor demoTraceInterceptor) {
        this.demoTraceInterceptor = demoTraceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoTraceInterceptor).addPathPatterns("/demo/**");
    }
}
