package com.xxxlog.client.trace;

import com.xxxlog.common.enums.TraceMode;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP 链路追踪过滤器（javax.servlet，适用于 Spring Boot 2.x / JDK 8）。
 */
public class TraceFilter implements Filter {

    private final TraceMode traceMode;

    public TraceFilter(TraceMode traceMode) {
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
