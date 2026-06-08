package com.xxxlog.server.dto;

import com.xxxlog.common.enums.KeywordOperator;

import java.util.ArrayList;
import java.util.List;

public class LogQueryRequest {

    private String appName;
    private String traceId;
    private String level;
    private String keyword;
    private List<String> keywords = new ArrayList<>();
    private KeywordOperator keywordOperator = KeywordOperator.AND;
    private String env;
    private Long startTime;
    private Long endTime;
    private int page = 1;
    private int size = 50;
    /** 时间排序：asc 正序，desc 倒序（默认） */
    private String sortOrder = "desc";

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public KeywordOperator getKeywordOperator() {
        return keywordOperator;
    }

    public void setKeywordOperator(KeywordOperator keywordOperator) {
        this.keywordOperator = keywordOperator;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
