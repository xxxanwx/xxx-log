package com.xxxlog.server.config;

import com.xxxlog.common.enums.ErrorAlertStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "xxx-log.dingtalk")
public class DingTalkProperties {

    private boolean enabled = false;
    private String webhookUrl = "";
    private String secret = "";
    private DailySummary dailySummary = new DailySummary();
    private ErrorAlert errorAlert = new ErrorAlert();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public DailySummary getDailySummary() {
        return dailySummary;
    }

    public void setDailySummary(DailySummary dailySummary) {
        this.dailySummary = dailySummary;
    }

    public ErrorAlert getErrorAlert() {
        return errorAlert;
    }

    public void setErrorAlert(ErrorAlert errorAlert) {
        this.errorAlert = errorAlert;
    }

    public static class DailySummary {

        private boolean enabled = true;
        /** 默认每天 09:00 推送昨日 ERROR 汇总 */
        private String cron = "0 0 9 * * ?";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class ErrorAlert {

        private boolean enabled = true;
        /** 推送策略：ALL | KEYWORD | BLACKLIST */
        private ErrorAlertStrategy strategy = ErrorAlertStrategy.KEYWORD;
        /** KEYWORD 模式：命中任一关键词才推送 */
        private List<String> keywords = new ArrayList<>();
        /** 黑名单：命中则不推送（三种模式均生效） */
        private List<String> blacklistKeywords = new ArrayList<>();
        /** 限定应用，为空表示全部应用 */
        private List<String> appNames = new ArrayList<>();
        /** 相同内容最短推送间隔（秒），防刷屏 */
        private int minIntervalSeconds = 60;
        /** 消息正文最大长度 */
        private int maxMessageLength = 500;
        private boolean includeStackTrace = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public ErrorAlertStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(ErrorAlertStrategy strategy) {
            this.strategy = strategy;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public List<String> getBlacklistKeywords() {
            return blacklistKeywords;
        }

        public void setBlacklistKeywords(List<String> blacklistKeywords) {
            this.blacklistKeywords = blacklistKeywords;
        }

        public List<String> getAppNames() {
            return appNames;
        }

        public void setAppNames(List<String> appNames) {
            this.appNames = appNames;
        }

        public int getMinIntervalSeconds() {
            return minIntervalSeconds;
        }

        public void setMinIntervalSeconds(int minIntervalSeconds) {
            this.minIntervalSeconds = minIntervalSeconds;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }

        public boolean isIncludeStackTrace() {
            return includeStackTrace;
        }

        public void setIncludeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
        }
    }
}
