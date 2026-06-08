package com.xxxlog.client.config;

import com.xxxlog.common.enums.TraceMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xxx-log")
public class XxxLogProperties {

    private boolean traceEnabled = true;
    private TraceMode traceMode = TraceMode.NATIVE;
    private String appName = "unknown";
    private String env = "default";

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public TraceMode getTraceMode() {
        return traceMode;
    }

    public void setTraceMode(TraceMode traceMode) {
        this.traceMode = traceMode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
