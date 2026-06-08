package com.xxxlog.client.cloud;

import com.xxxlog.client.config.XxxLogProperties;
import com.xxxlog.client.trace.TraceContext;
import com.xxxlog.client.trace.TracePropagation;
import com.xxxlog.common.enums.TraceMode;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Spring Cloud Gateway 入口链路解析与向下游透传。
 */
public class GatewayTraceGlobalFilter implements GlobalFilter, Ordered {

    private final TraceMode traceMode;

    public GatewayTraceGlobalFilter(XxxLogProperties properties) {
        TraceMode mode = properties.getTraceMode();
        this.traceMode = mode == null ? TraceMode.COMPATIBLE : mode;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        TracePropagation.applyInbound(request.getHeaders()::getFirst, traceMode);

        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> TracePropagation.writeOutbound(headers::set, traceMode))
                .build();

        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> {
                    TraceContext.clear();
                    MDC.clear();
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
