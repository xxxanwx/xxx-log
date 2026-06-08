package com.xxxlog.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 演示 xxx-log 链路追踪：
 * <ul>
 *   <li>TraceFilter：请求入口自动生成/透传 traceId（见 xxx-log-client 自动配置）</li>
 *   <li>DemoTraceInterceptor：Interceptor 层打印 traceId</li>
 *   <li>/demo/chain：Controller + Service 多 span 链路演示</li>
 * </ul>
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
