package com.xxxlog.common.constant;

public final class LogConstants {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_PARENT_SPAN_ID = "parentSpanId";

    public static final String DEFAULT_REDIS_QUEUE = "xxx-log:queue";
    public static final String DEFAULT_RABBITMQ_QUEUE = "xxx-log.queue";

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_SPAN_ID = "X-Span-Id";

    public static final String HEADER_B3_TRACE_ID = "X-B3-TraceId";
    public static final String HEADER_B3_SPAN_ID = "X-B3-SpanId";
    public static final String HEADER_B3_SAMPLED = "X-B3-Sampled";

    public static final String HEADER_TRACEPARENT = "traceparent";

    private LogConstants() {
    }
}
