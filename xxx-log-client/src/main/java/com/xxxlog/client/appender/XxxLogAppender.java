package com.xxxlog.client.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.xxxlog.client.sender.LogSender;
import com.xxxlog.client.sender.RabbitMqLogSender;
import com.xxxlog.client.sender.RedisLogSender;
import com.xxxlog.common.constant.LogConstants;
import com.xxxlog.common.enums.QueueType;
import com.xxxlog.common.model.LogRecord;
import com.xxxlog.common.util.StringUtil;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logback Appender，将日志异步推送到 Redis 或 RabbitMQ 队列。
 */
public class XxxLogAppender extends AppenderBase<ILoggingEvent> {

    private String appName = "unknown";
    private String env = "default";
    private String queueType = QueueType.REDIS.name().toLowerCase();

    private String redisHost = "127.0.0.1";
    private int redisPort = 6379;
    private String redisPassword = "";
    private int redisDatabase = 0;
    private String queueKey = LogConstants.DEFAULT_REDIS_QUEUE;

    private String rabbitmqHost = "127.0.0.1";
    private int rabbitmqPort = 5672;
    private String rabbitmqUsername = "guest";
    private String rabbitmqPassword = "guest";
    private String rabbitmqVirtualHost = "/";
    private String rabbitmqQueue = LogConstants.DEFAULT_RABBITMQ_QUEUE;

    private volatile LogSender logSender;
    private volatile String serverIp;

    @Override
    public void start() {
        if (StringUtil.isBlank(appName)) {
            addError("appName is required");
            return;
        }
        try {
            logSender = createLogSender();
        } catch (Exception e) {
            addError("Failed to create log sender", e);
            return;
        }
        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            serverIp = "unknown";
            addWarn("Failed to resolve server IP", e);
        }
        super.start();
    }

    private LogSender createLogSender() {
        QueueType type = parseQueueType(queueType);
        if (type == QueueType.RABBITMQ) {
            return new RabbitMqLogSender(
                    rabbitmqHost,
                    rabbitmqPort,
                    rabbitmqUsername,
                    rabbitmqPassword,
                    rabbitmqVirtualHost,
                    rabbitmqQueue);
        }
        return new RedisLogSender(redisHost, redisPort, redisPassword, redisDatabase, queueKey);
    }

    private QueueType parseQueueType(String value) {
        if (StringUtil.isBlank(value)) {
            return QueueType.REDIS;
        }
        try {
            return QueueType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            addWarn("Unknown queueType: " + value + ", fallback to REDIS");
            return QueueType.REDIS;
        }
    }

    @Override
    public void stop() {
        if (logSender != null) {
            logSender.close();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted() || logSender == null) {
            return;
        }
        try {
            logSender.send(buildRecord(event));
        } catch (Exception e) {
            addError("Failed to send log", e);
        }
    }

    private LogRecord buildRecord(ILoggingEvent event) {
        LogRecord record = new LogRecord();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setAppName(appName);
        record.setEnv(env);
        record.setServerIp(serverIp);
        record.setThreadName(event.getThreadName());
        record.setLoggerName(event.getLoggerName());
        record.setLevel(event.getLevel().toString());
        record.setMessage(event.getFormattedMessage());
        record.setTimestamp(event.getTimeStamp());

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            record.setMdc(new HashMap<String, String>(mdc));
            record.setTraceId(mdc.get(LogConstants.MDC_TRACE_ID));
            record.setSpanId(mdc.get(LogConstants.MDC_SPAN_ID));
            record.setParentSpanId(mdc.get(LogConstants.MDC_PARENT_SPAN_ID));
        }

        StackTraceElement[] caller = event.getCallerData();
        if (caller != null && caller.length > 0) {
            record.setClassName(caller[0].getClassName());
            record.setMethodName(caller[0].getMethodName());
        }

        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            record.setStackTrace(ThrowableProxyUtil.asString(throwable));
        }
        return record;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public void setQueueType(String queueType) {
        this.queueType = queueType;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public void setRedisPort(int redisPort) {
        this.redisPort = redisPort;
    }

    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword == null ? "" : redisPassword.trim();
    }

    public void setRedisDatabase(int redisDatabase) {
        this.redisDatabase = redisDatabase;
    }

    public void setQueueKey(String queueKey) {
        this.queueKey = queueKey;
    }

    public void setRabbitmqHost(String rabbitmqHost) {
        this.rabbitmqHost = rabbitmqHost;
    }

    public void setRabbitmqPort(int rabbitmqPort) {
        this.rabbitmqPort = rabbitmqPort;
    }

    public void setRabbitmqUsername(String rabbitmqUsername) {
        this.rabbitmqUsername = rabbitmqUsername;
    }

    public void setRabbitmqPassword(String rabbitmqPassword) {
        this.rabbitmqPassword = rabbitmqPassword == null ? "" : rabbitmqPassword.trim();
    }

    public void setRabbitmqVirtualHost(String rabbitmqVirtualHost) {
        this.rabbitmqVirtualHost = rabbitmqVirtualHost;
    }

    public void setRabbitmqQueue(String rabbitmqQueue) {
        this.rabbitmqQueue = rabbitmqQueue;
    }
}
