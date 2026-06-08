package com.xxxlog.server.dto;

public class LogIndexInfoDto {

    private String indexName;
    private String appName;
    private String indexTime;
    private long docCount;
    /** 人类可读大小，如 100mb、1.2gb */
    private String storeSize;
    private long storeSizeBytes;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getIndexTime() {
        return indexTime;
    }

    public void setIndexTime(String indexTime) {
        this.indexTime = indexTime;
    }

    public long getDocCount() {
        return docCount;
    }

    public void setDocCount(long docCount) {
        this.docCount = docCount;
    }

    public String getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(String storeSize) {
        this.storeSize = storeSize;
    }

    public long getStoreSizeBytes() {
        return storeSizeBytes;
    }

    public void setStoreSizeBytes(long storeSizeBytes) {
        this.storeSizeBytes = storeSizeBytes;
    }
}
