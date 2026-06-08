package com.xxxlog.client.trace;

/**
 * 写入 HTTP 响应/出站请求头。
 */
public interface TraceHeaderWriter {

    void setHeader(String name, String value);
}
