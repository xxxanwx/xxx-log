package com.xxxlog.common.enums;

/**
 * 链路追踪模式。
 * <ul>
 *   <li>NATIVE — 仅 X-Trace-Id / X-Span-Id，适合单体</li>
 *   <li>COMPATIBLE — 同时读写 B3 / W3C，适合 Spring Cloud 微服务</li>
 *   <li>MICROMETER — 交给 Micrometer Tracing，关闭内置 TraceFilter</li>
 * </ul>
 */
public enum TraceMode {

    NATIVE,
    COMPATIBLE,
    MICROMETER
}
