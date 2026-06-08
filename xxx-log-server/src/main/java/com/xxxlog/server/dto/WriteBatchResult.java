package com.xxxlog.server.dto;

public class WriteBatchResult {

    private int written;
    private int skipped;
    private int total;

    public WriteBatchResult() {
    }

    public WriteBatchResult(int written, int skipped, int total) {
        this.written = written;
        this.skipped = skipped;
        this.total = total;
    }

    public int getWritten() {
        return written;
    }

    public void setWritten(int written) {
        this.written = written;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean hasWritten() {
        return written > 0;
    }
}
