package com.xxxlog.server.controller;

import com.xxxlog.common.model.LogRecord;
import com.xxxlog.server.dto.ApiResponse;
import com.xxxlog.server.dto.LogQueryRequest;
import com.xxxlog.server.dto.PageResult;
import com.xxxlog.server.dto.QueueStatsDto;
import com.xxxlog.server.service.LogQueryService;
import com.xxxlog.server.service.QueueStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogQueryController {

    private final LogQueryService logQueryService;
    private final QueueStatsService queueStatsService;

    public LogQueryController(LogQueryService logQueryService, QueueStatsService queueStatsService) {
        this.logQueryService = logQueryService;
        this.queueStatsService = queueStatsService;
    }

    @PostMapping("/search")
    public ApiResponse<PageResult<LogRecord>> search(@RequestBody LogQueryRequest request) {
        return ApiResponse.ok(logQueryService.search(request));
    }

    @GetMapping("/trace/{traceId}")
    public ApiResponse<List<LogRecord>> trace(
            @PathVariable("traceId") String traceId,
            @RequestParam(value = "appName", required = false) String appName,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime) {
        return ApiResponse.ok(logQueryService.searchByTraceId(traceId, appName, startTime, endTime));
    }

    @GetMapping("/apps")
    public ApiResponse<List<String>> apps() {
        return ApiResponse.ok(logQueryService.listAppNames());
    }

    @GetMapping("/envs")
    public ApiResponse<List<String>> envs() {
        return ApiResponse.ok(logQueryService.listEnvs());
    }

    @GetMapping("/queue/stats")
    public ApiResponse<QueueStatsDto> queueStats() {
        return ApiResponse.ok(queueStatsService.getStats());
    }
}
