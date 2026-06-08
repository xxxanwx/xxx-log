package com.xxxlog.server.dto;

public class QueueStatsDto {

    private String queueType;
    private String queueName;
    private long queuePending;
    private long bufferPending;
    private long totalPending;

    public String getQueueType() {
        return queueType;
    }

    public void setQueueType(String queueType) {
        this.queueType = queueType;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public long getQueuePending() {
        return queuePending;
    }

    public void setQueuePending(long queuePending) {
        this.queuePending = queuePending;
    }

    public long getBufferPending() {
        return bufferPending;
    }

    public void setBufferPending(long bufferPending) {
        this.bufferPending = bufferPending;
    }

    public long getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(long totalPending) {
        this.totalPending = totalPending;
    }
}
