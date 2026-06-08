package com.xxxlog.server.controller;

import com.xxxlog.server.dto.ApiResponse;
import com.xxxlog.server.dto.LogIndexManageDto;
import com.xxxlog.server.service.LogIndexService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs/indices")
public class LogIndexController {

    private final LogIndexService logIndexService;

    public LogIndexController(LogIndexService logIndexService) {
        this.logIndexService = logIndexService;
    }

    @GetMapping
    public ApiResponse<LogIndexManageDto> list() {
        return ApiResponse.ok(logIndexService.listManageInfo());
    }

    @DeleteMapping("/{indexName}")
    public ApiResponse<Void> delete(@PathVariable("indexName") String indexName) {
        logIndexService.deleteIndex(indexName);
        return ApiResponse.ok(null);
    }
}
