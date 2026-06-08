package com.xxxlog.client.cloud;

import com.xxxlog.client.trace.TracePropagation;
import com.xxxlog.common.enums.TraceMode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 出站链路头透传。
 */
public class WebClientTraceFilter implements ExchangeFilterFunction {

    private final TraceMode traceMode;

    public WebClientTraceFilter(TraceMode traceMode) {
        this.traceMode = traceMode == null ? TraceMode.COMPATIBLE : traceMode;
    }

    @Override
    public Mono<org.springframework.web.reactive.function.client.ClientResponse> filter(
            ClientRequest request, ExchangeFunction next) {
        TracePropagation.ensureTraceStarted();
        ClientRequest.Builder builder = ClientRequest.from(request);
        TracePropagation.writeOutbound(builder::header, traceMode);
        return next.exchange(builder.build());
    }
}
