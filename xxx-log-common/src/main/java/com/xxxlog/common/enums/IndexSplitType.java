package com.xxxlog.common.enums;

/**
 * ES 索引按时间维度拆分策略。
 */
public enum IndexSplitType {

    HOUR("yyyy-MM-dd-HH"),
    DAY("yyyy-MM-dd"),
    MONTH("yyyy-MM");

    private final String pattern;

    IndexSplitType(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}
