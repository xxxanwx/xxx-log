package com.xxxlog.server.dto;

public class TopErrorDto {

    private String message;
    private String appName;
    private long count;

    public TopErrorDto() {
    }

    public TopErrorDto(String message, String appName, long count) {
        this.message = message;
        this.appName = appName;
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
