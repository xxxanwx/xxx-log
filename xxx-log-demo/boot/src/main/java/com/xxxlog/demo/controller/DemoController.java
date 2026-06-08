package com.xxxlog.demo.controller;

import com.xxxlog.client.trace.TraceContext;
import com.xxxlog.demo.service.DemoChainService;
import com.xxxlog.demo.support.TraceStepRecorder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final DemoChainService demoChainService;
    private final TraceStepRecorder stepRecorder;

    public DemoController(DemoChainService demoChainService, TraceStepRecorder stepRecorder) {
        this.demoChainService = demoChainService;
        this.stepRecorder = stepRecorder;
    }

    /**
     * 简单接口：traceId 由 TraceFilter 在请求入口自动生成。
     */
    @GetMapping("/hello")
    public Map<String, Object> hello(@RequestParam(value = "name", defaultValue = "world") String name) {
        log.info("[Controller] 收到 hello 请求, name={}", name);
        simulateWork();

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("message", "Hello, " + name);
        result.put("traceId", TraceContext.getTraceId());
        result.put("spanId", TraceContext.getSpanId());
        result.put("tip", "在 xxx-log-web 中用 traceId 搜索即可查看该请求全链路日志");
        return result;
    }

    /**
     * 链路演示：Filter -> Interceptor -> Controller -> Service(多 span)。
     */
    @GetMapping("/chain")
    public Map<String, Object> chain(@RequestParam(value = "orderId", defaultValue = "ORD-20260608-001") String orderId,
                                       HttpServletRequest request) {
        log.info("[Controller] 收到链路演示请求, orderId={}", orderId);
        stepRecorder.record("controller.chain");
        demoChainService.processOrder(orderId);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("orderId", orderId);
        result.put("traceId", TraceContext.getTraceId());
        result.put("responseHeaderTraceId", request.getHeader("X-Trace-Id"));
        result.put("steps", stepRecorder.getSteps());
        result.put("tip", "同一 traceId 下各阶段 spanId 不同；打开 xxx-log-web -> 链路视图查看时间线");
        return result;
    }

    /**
     * 查看当前请求的追踪上下文（Filter 写入 MDC 后的值）。
     */
    @GetMapping("/trace")
    public Map<String, Object> traceInfo(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("traceId", TraceContext.getTraceId());
        result.put("spanId", TraceContext.getSpanId());
        result.put("requestHeaderTraceId", request.getHeader("X-Trace-Id"));
        result.put("requestHeaderSpanId", request.getHeader("X-Span-Id"));
        result.put("note", "无请求头时 TraceFilter 自动生成；有 X-Trace-Id 时透传上游链路");
        return result;
    }

    @GetMapping("/error")
    public Map<String, Object> error() {
        log.warn("[Controller] 即将触发异常, traceId={}", TraceContext.getTraceId());
        throw new RuntimeException("模拟业务异常, traceId=" + TraceContext.getTraceId());
    }

    private void simulateWork() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
