package com.xxxlog.server.dto;

import java.util.List;
import java.util.Map;

public class DashboardOverviewDto {

    private long totalLogs;
    private long errorCount;
    private long warnCount;
    private Map<String, Long> errorsByApp;

    public long getTotalLogs() {
        return totalLogs;
    }

    public void setTotalLogs(long totalLogs) {
        this.totalLogs = totalLogs;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(long warnCount) {
        this.warnCount = warnCount;
    }

    public Map<String, Long> getErrorsByApp() {
        return errorsByApp;
    }

    public void setErrorsByApp(Map<String, Long> errorsByApp) {
        this.errorsByApp = errorsByApp;
    }
}
