package com.xxxlog.client.trace;

import com.xxxlog.common.enums.TraceMode;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP 链路追踪过滤器（jakarta.servlet，适用于 Spring Boot 3.x / JDK 17+）。
 */
public class JakartaTraceFilter implements Filter {

    private final TraceMode traceMode;

    public JakartaTraceFilter(TraceMode traceMode) {
        this.traceMode = traceMode == null ? TraceMode.NATIVE : traceMode;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            TracePropagation.applyInbound(httpRequest::getHeader, traceMode);
            TracePropagation.writeOutbound(httpResponse::setHeader, traceMode);
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
