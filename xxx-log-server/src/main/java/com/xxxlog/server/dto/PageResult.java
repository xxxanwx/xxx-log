package com.xxxlog.server.dto;

import java.util.List;

public class PageResult<T> {

    private long total;
    private int page;
    private int size;
    private List<T> records;
    /** 下一页 search_after 游标 */
    private List<Object> nextSearchAfter;
    /** 是否处于深度分页模式 */
    private boolean deepPagination;

    public PageResult() {
    }

    public PageResult(long total, int page, int size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public List<Object> getNextSearchAfter() {
        return nextSearchAfter;
    }

    public void setNextSearchAfter(List<Object> nextSearchAfter) {
        this.nextSearchAfter = nextSearchAfter;
    }

    public boolean isDeepPagination() {
        return deepPagination;
    }

    public void setDeepPagination(boolean deepPagination) {
        this.deepPagination = deepPagination;
    }
}
