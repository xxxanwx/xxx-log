package com.xxxlog.demo.cloud.controller;

import com.xxxlog.client.trace.TraceContext;
import com.xxxlog.demo.cloud.client.InternalFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoCloudController {

    private static final Logger log = LoggerFactory.getLogger(DemoCloudController.class);

    private final InternalFeignClient internalFeignClient;

    public DemoCloudController(InternalFeignClient internalFeignClient) {
        this.internalFeignClient = internalFeignClient;
    }

    @GetMapping("/feign")
    public Map<String, String> feignCall() {
        log.info("Gateway service received feign demo request");
        String downstreamTraceId = internalFeignClient.internalHello();
        Map<String, String> result = new HashMap<String, String>();
        result.put("upstreamTraceId", TraceContext.getTraceId());
        result.put("downstreamResponse", downstreamTraceId);
        return result;
    }

    @GetMapping("/internal")
    public String internalHello() {
        log.info("Internal service handling request");
        return TraceContext.getTraceId();
    }
}
