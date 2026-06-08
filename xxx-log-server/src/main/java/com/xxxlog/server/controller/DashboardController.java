package com.xxxlog.server.controller;

import com.xxxlog.server.dto.ApiResponse;
import com.xxxlog.server.dto.DashboardOverviewDto;
import com.xxxlog.server.dto.TopErrorDto;
import com.xxxlog.server.dto.TrendPointDto;
import com.xxxlog.server.service.LogStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final LogStatisticsService logStatisticsService;

    public DashboardController(LogStatisticsService logStatisticsService) {
        this.logStatisticsService = logStatisticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewDto> overview(
            @RequestParam("startTime") long startTime,
            @RequestParam("endTime") long endTime) {
        return ApiResponse.ok(logStatisticsService.overview(startTime, endTime));
    }

    @GetMapping("/error-trend")
    public ApiResponse<List<TrendPointDto>> errorTrend(
            @RequestParam("startTime") long startTime,
            @RequestParam("endTime") long endTime,
            @RequestParam(value = "interval", defaultValue = "hour") String interval) {
        return ApiResponse.ok(logStatisticsService.errorTrend(startTime, endTime, interval));
    }

    @GetMapping("/ingest-trend")
    public ApiResponse<List<TrendPointDto>> ingestTrend(
            @RequestParam("startTime") long startTime,
            @RequestParam("endTime") long endTime,
            @RequestParam(value = "interval", defaultValue = "hour") String interval) {
        return ApiResponse.ok(logStatisticsService.ingestTrend(startTime, endTime, interval));
    }

    @GetMapping("/top-errors")
    public ApiResponse<List<TopErrorDto>> topErrors(
            @RequestParam("startTime") long startTime,
            @RequestParam("endTime") long endTime,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.ok(logStatisticsService.topErrors(startTime, endTime, limit));
    }
}
