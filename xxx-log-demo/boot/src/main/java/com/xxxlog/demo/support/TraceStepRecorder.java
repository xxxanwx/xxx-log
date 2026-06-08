package com.xxxlog.demo.support;

import com.xxxlog.client.trace.TraceContext;
import com.xxxlog.common.constant.LogConstants;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录单次请求内各阶段的 traceId / spanId，用于接口返回演示。
 */
@Component
@RequestScope
public class TraceStepRecorder {

    private final List<Map<String, String>> steps = new ArrayList<Map<String, String>>();

    public void record(String stage) {
        Map<String, String> step = new LinkedHashMap<String, String>();
        step.put("stage", stage);
        step.put("traceId", TraceContext.getTraceId());
        step.put("spanId", TraceContext.getSpanId());
        step.put("parentSpanId", MDC.get(LogConstants.MDC_PARENT_SPAN_ID));
        steps.add(step);
    }

    public List<Map<String, String>> getSteps() {
        return new ArrayList<Map<String, String>>(steps);
    }
}
