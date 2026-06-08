package com.xxxlog.server.dto;

public class TrendPointDto {

    private long time;
    private long count;

    public TrendPointDto() {
    }

    public TrendPointDto(long time, long count) {
        this.time = time;
        this.count = count;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
