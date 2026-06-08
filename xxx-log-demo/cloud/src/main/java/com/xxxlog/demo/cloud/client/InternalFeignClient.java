package com.xxxlog.demo.cloud.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "demo-internal", url = "http://127.0.0.1:8080")
public interface InternalFeignClient {

    @GetMapping("/demo/internal")
    String internalHello();
}
