package com.xxxlog.demo.interceptor;

import com.xxxlog.client.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 演示：Filter 之后 Interceptor 阶段即可拿到 traceId（由 xxx-log TraceFilter 在请求入口写入 MDC）。
 */
@Component
public class DemoTraceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DemoTraceInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("[Interceptor] 请求进入 uri={}, traceId={}, spanId={}",
                request.getRequestURI(),
                TraceContext.getTraceId(),
                TraceContext.getSpanId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.info("[Interceptor] 请求结束 uri={}, traceId={}, status={}",
                request.getRequestURI(),
                TraceContext.getTraceId(),
                response.getStatus());
    }
}
