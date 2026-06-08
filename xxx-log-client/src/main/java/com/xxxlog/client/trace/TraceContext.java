package com.xxxlog.client.trace;

import com.xxxlog.common.constant.LogConstants;
import com.xxxlog.common.util.StringUtil;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪上下文，基于 SLF4J MDC 实现。
 */
public final class TraceContext {

    private TraceContext() {
    }

    public static String startTrace() {
        String traceId = generateId();
        MDC.put(LogConstants.MDC_TRACE_ID, traceId);
        MDC.put(LogConstants.MDC_SPAN_ID, generateId());
        MDC.remove(LogConstants.MDC_PARENT_SPAN_ID);
        return traceId;
    }

    public static void startSpan() {
        String currentSpanId = MDC.get(LogConstants.MDC_SPAN_ID);
        if (currentSpanId != null) {
            MDC.put(LogConstants.MDC_PARENT_SPAN_ID, currentSpanId);
        }
        MDC.put(LogConstants.MDC_SPAN_ID, generateId());
    }

    public static void setTraceId(String traceId) {
        if (StringUtil.isNotBlank(traceId)) {
            MDC.put(LogConstants.MDC_TRACE_ID, traceId);
        }
    }

    public static String getTraceId() {
        return MDC.get(LogConstants.MDC_TRACE_ID);
    }

    public static String getSpanId() {
        return MDC.get(LogConstants.MDC_SPAN_ID);
    }

    public static void clear() {
        MDC.remove(LogConstants.MDC_TRACE_ID);
        MDC.remove(LogConstants.MDC_SPAN_ID);
        MDC.remove(LogConstants.MDC_PARENT_SPAN_ID);
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
