package com.xxxlog.demo.service;

import com.xxxlog.client.trace.TraceContext;
import com.xxxlog.demo.support.TraceStepRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 模拟多层业务调用，产生父子 span，便于在查询界面按 traceId 查看链路。
 */
@Service
public class DemoChainService {

    private static final Logger log = LoggerFactory.getLogger(DemoChainService.class);

    private final TraceStepRecorder stepRecorder;

    public DemoChainService(TraceStepRecorder stepRecorder) {
        this.stepRecorder = stepRecorder;
    }

    public void processOrder(String orderId) {
        log.info("[Service] 开始处理订单 orderId={}", orderId);
        stepRecorder.record("service.processOrder");

        validateOrder(orderId);
        saveOrder(orderId);
        notifyOrder(orderId);

        log.info("[Service] 订单处理完成 orderId={}", orderId);
    }

    private void validateOrder(String orderId) {
        TraceContext.startSpan();
        log.info("[Service] 校验订单 orderId={}", orderId);
        stepRecorder.record("service.validateOrder");
        sleepQuietly(30);
    }

    private void saveOrder(String orderId) {
        TraceContext.startSpan();
        log.error("[Service] 保存订单 orderId={}", orderId);
        stepRecorder.record("service.saveOrder");
        sleepQuietly(40);
    }

    private void notifyOrder(String orderId) {
        TraceContext.startSpan();
        log.info("[Service] 发送通知 orderId={}", orderId);
        stepRecorder.record("service.notifyOrder");
        sleepQuietly(20);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
