package com.xxxlog.server.dto;

import java.util.List;

public class LogIndexManageDto {

    private List<LogIndexInfoDto> indices;
    private boolean retentionEnabled;
    private int retentionDays;

    public List<LogIndexInfoDto> getIndices() {
        return indices;
    }

    public void setIndices(List<LogIndexInfoDto> indices) {
        this.indices = indices;
    }

    public boolean isRetentionEnabled() {
        return retentionEnabled;
    }

    public void setRetentionEnabled(boolean retentionEnabled) {
        this.retentionEnabled = retentionEnabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
