package com.xxxlog.common.util;

import com.xxxlog.common.model.LogRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 基于日志内容生成稳定的文档 ID，避免重复写入。
 */
public final class LogRecordIdResolver {

    private LogRecordIdResolver() {
    }

    public static String resolve(LogRecord record) {
        if (record == null) {
            return sha256("");
        }
        String input = String.join("|",
                nullToEmpty(record.getAppName()),
                nullToEmpty(record.getEnv()),
                String.valueOf(record.getTimestamp()),
                nullToEmpty(record.getThreadName()),
                nullToEmpty(record.getLoggerName()),
                nullToEmpty(record.getMessage()));
        return sha256(input);
    }

    public static void ensureId(LogRecord record) {
        if (record == null) {
            return;
        }
        if (StringUtil.isBlank(record.getId())) {
            record.setId(resolve(record));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
