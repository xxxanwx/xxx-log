package com.xxxlog.client.cloud;

import com.xxxlog.client.trace.TraceHeaderWriter;
import com.xxxlog.client.trace.TracePropagation;
import com.xxxlog.common.enums.TraceMode;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign 出站链路头透传。
 */
public class FeignTraceInterceptor implements RequestInterceptor {

    private final TraceMode traceMode;

    public FeignTraceInterceptor(TraceMode traceMode) {
        this.traceMode = traceMode == null ? TraceMode.COMPATIBLE : traceMode;
    }

    @Override
    public void apply(RequestTemplate template) {
        TracePropagation.ensureTraceStarted();
        TracePropagation.writeOutbound(new TraceHeaderWriter() {
            @Override
            public void setHeader(String name, String value) {
                template.header(name, value);
            }
        }, traceMode);
    }
}
