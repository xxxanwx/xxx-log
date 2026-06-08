package com.xxxlog.common.enums;

/**
 * ERROR 实时告警推送策略。
 * <ul>
 *   <li>ALL — 所有 ERROR 都推送（仍受黑名单关键词过滤）</li>
 *   <li>KEYWORD — 仅 message / stackTrace 命中配置关键词时推送（推荐，避免刷屏）</li>
 *   <li>BLACKLIST — 除黑名单外所有 ERROR 都推送</li>
 * </ul>
 */
public enum ErrorAlertStrategy {

    ALL,
    KEYWORD,
    BLACKLIST
}
