package com.xxxlog.common.sampling;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 日志采样策略：sampleRate 0-100 表示通过百分比，ERROR/WARN 始终通过。
 */
public class LogSamplingPolicy {

    private static final Set<String> ALWAYS_PASS_LEVELS = new HashSet<>(
            Arrays.asList("ERROR", "WARN"));

    private final int sampleRate;

    public LogSamplingPolicy(int sampleRate) {
        this.sampleRate = Math.max(0, Math.min(100, sampleRate));
    }

    /**
     * @return true 表示应发送该条日志
     */
    public boolean shouldPass(String level) {
        if (level != null && ALWAYS_PASS_LEVELS.contains(level.toUpperCase())) {
            return true;
        }
        if (sampleRate >= 100) {
            return true;
        }
        if (sampleRate <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextInt(100) < sampleRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
