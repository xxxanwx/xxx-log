package com.xxxlog.client.cloud;

import com.xxxlog.client.trace.TracePropagation;
import com.xxxlog.common.enums.TraceMode;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate 出站链路头透传。
 */
public class TraceRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final TraceMode traceMode;

    public TraceRestTemplateInterceptor(TraceMode traceMode) {
        this.traceMode = traceMode == null ? TraceMode.COMPATIBLE : traceMode;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        TracePropagation.ensureTraceStarted();
        TracePropagation.writeOutbound(request.getHeaders()::set, traceMode);
        return execution.execute(request, body);
    }
}
