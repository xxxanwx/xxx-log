package com.xxxlog.client.trace;

import com.xxxlog.common.constant.LogConstants;
import com.xxxlog.common.enums.TraceMode;
import com.xxxlog.common.util.StringUtil;

/**
 * 链路 ID 提取与透传，兼容原生 / B3 / W3C 格式。
 */
public final class TracePropagation {

    private TracePropagation() {
    }

    public static void applyInbound(TraceHeaderReader reader, TraceMode mode) {
        String traceId = null;
        String spanId = null;

        if (mode == TraceMode.COMPATIBLE) {
            traceId = firstNonBlank(
                    reader.getHeader(LogConstants.HEADER_TRACE_ID),
                    reader.getHeader(LogConstants.HEADER_B3_TRACE_ID),
                    parseW3cTraceId(reader.getHeader(LogConstants.HEADER_TRACEPARENT)));
            spanId = firstNonBlank(
                    reader.getHeader(LogConstants.HEADER_SPAN_ID),
                    reader.getHeader(LogConstants.HEADER_B3_SPAN_ID),
                    parseW3cSpanId(reader.getHeader(LogConstants.HEADER_TRACEPARENT)));
        } else {
            traceId = reader.getHeader(LogConstants.HEADER_TRACE_ID);
            spanId = reader.getHeader(LogConstants.HEADER_SPAN_ID);
        }

        if (StringUtil.isBlank(traceId)) {
            TraceContext.startTrace();
            return;
        }

        TraceContext.setTraceId(normalizeTraceId(traceId));
        if (StringUtil.isNotBlank(spanId)) {
            org.slf4j.MDC.put(LogConstants.MDC_SPAN_ID, normalizeSpanId(spanId));
        } else {
            TraceContext.startSpan();
        }
    }

    public static void writeOutbound(TraceHeaderWriter writer, TraceMode mode) {
        String traceId = TraceContext.getTraceId();
        String spanId = TraceContext.getSpanId();
        if (StringUtil.isBlank(traceId)) {
            traceId = TraceContext.startTrace();
            spanId = TraceContext.getSpanId();
        }

        writer.setHeader(LogConstants.HEADER_TRACE_ID, traceId);
        if (StringUtil.isNotBlank(spanId)) {
            writer.setHeader(LogConstants.HEADER_SPAN_ID, spanId);
        }

        if (mode == TraceMode.COMPATIBLE) {
            writer.setHeader(LogConstants.HEADER_B3_TRACE_ID, toB3TraceId(traceId));
            if (StringUtil.isNotBlank(spanId)) {
                writer.setHeader(LogConstants.HEADER_B3_SPAN_ID, toB3SpanId(spanId));
            }
            writer.setHeader(LogConstants.HEADER_B3_SAMPLED, "1");
            if (StringUtil.isNotBlank(spanId)) {
                writer.setHeader(LogConstants.HEADER_TRACEPARENT, toTraceParent(traceId, spanId));
            }
        }
    }

    public static void ensureTraceStarted() {
        if (StringUtil.isBlank(TraceContext.getTraceId())) {
            TraceContext.startTrace();
        }
    }

    private static String parseW3cTraceId(String traceParent) {
        if (StringUtil.isBlank(traceParent)) {
            return null;
        }
        String[] parts = traceParent.trim().split("-");
        if (parts.length >= 2 && parts[1].length() == 32) {
            return parts[1];
        }
        return null;
    }

    private static String parseW3cSpanId(String traceParent) {
        if (StringUtil.isBlank(traceParent)) {
            return null;
        }
        String[] parts = traceParent.trim().split("-");
        if (parts.length >= 3 && parts[2].length() == 16) {
            return parts[2];
        }
        return null;
    }

    private static String toTraceParent(String traceId, String spanId) {
        return "00-" + toB3TraceId(traceId) + "-" + toB3SpanId(spanId) + "-01";
    }

    private static String toB3TraceId(String traceId) {
        String normalized = normalizeTraceId(traceId);
        if (normalized.length() == 32) {
            return normalized.substring(16);
        }
        return normalized;
    }

    private static String toB3SpanId(String spanId) {
        String normalized = normalizeSpanId(spanId);
        if (normalized.length() >= 16) {
            return normalized.substring(normalized.length() - 16);
        }
        return normalized;
    }

    private static String normalizeTraceId(String traceId) {
        String value = traceId.trim().toLowerCase();
        if (value.length() == 16) {
            return "0000000000000000" + value;
        }
        return value;
    }

    private static String normalizeSpanId(String spanId) {
        String value = spanId.trim().toLowerCase();
        if (value.length() == 16) {
            return "0000000000000000" + value;
        }
        return value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
