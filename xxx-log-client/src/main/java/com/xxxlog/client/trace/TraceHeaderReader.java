package com.xxxlog.client.trace;

/**
 * 读取 HTTP 请求头，供 Servlet / Gateway / Feign 等场景复用。
 */
public interface TraceHeaderReader {

    String getHeader(String name);
}
