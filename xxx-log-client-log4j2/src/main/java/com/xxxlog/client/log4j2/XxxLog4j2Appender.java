package com.xxxlog.client.log4j2;

import com.xxxlog.client.sender.LogSender;
import com.xxxlog.client.sender.RabbitMqLogSender;
import com.xxxlog.client.sender.RedisLogSender;
import com.xxxlog.common.constant.LogConstants;
import com.xxxlog.common.enums.QueueType;
import com.xxxlog.common.model.LogRecord;
import com.xxxlog.common.sampling.LogSamplingPolicy;
import com.xxxlog.common.util.LogRecordIdResolver;
import com.xxxlog.common.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Log4j2 Appender，将日志异步推送到 Redis 或 RabbitMQ 队列。
 */
@Plugin(name = "XxxLog4j2", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class XxxLog4j2Appender extends AbstractAppender {

    private static final Logger LOGGER = LogManager.getLogger(XxxLog4j2Appender.class);

    private final String appName;
    private final String env;
    private final LogSamplingPolicy samplingPolicy;
    private final LogSender logSender;
    private final String serverIp;

    protected XxxLog4j2Appender(String name,
                                Filter filter,
                                String appName,
                                String env,
                                int sampleRate,
                                LogSender logSender,
                                String serverIp) {
        super(name, filter, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        this.appName = appName;
        this.env = env;
        this.samplingPolicy = new LogSamplingPolicy(sampleRate);
        this.logSender = logSender;
        this.serverIp = serverIp;
    }

    @PluginFactory
    public static XxxLog4j2Appender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute(value = "appName") String appName,
            @PluginAttribute(value = "env", defaultString = "default") String env,
            @PluginAttribute(value = "queueType", defaultString = "redis") String queueType,
            @PluginAttribute(value = "redisHost", defaultString = "127.0.0.1") String redisHost,
            @PluginAttribute(value = "redisPort", defaultInt = 6379) int redisPort,
            @PluginAttribute(value = "redisPassword", defaultString = "") String redisPassword,
            @PluginAttribute(value = "redisDatabase", defaultInt = 0) int redisDatabase,
            @PluginAttribute(value = "queueKey", defaultString = LogConstants.DEFAULT_REDIS_QUEUE) String queueKey,
            @PluginAttribute(value = "rabbitmqHost", defaultString = "127.0.0.1") String rabbitmqHost,
            @PluginAttribute(value = "rabbitmqPort", defaultInt = 5672) int rabbitmqPort,
            @PluginAttribute(value = "rabbitmqUsername", defaultString = "guest") String rabbitmqUsername,
            @PluginAttribute(value = "rabbitmqPassword", defaultString = "guest") String rabbitmqPassword,
            @PluginAttribute(value = "rabbitmqVirtualHost", defaultString = "/") String rabbitmqVirtualHost,
            @PluginAttribute(value = "rabbitmqQueue", defaultString = LogConstants.DEFAULT_RABBITMQ_QUEUE) String rabbitmqQueue,
            @PluginAttribute(value = "sampleRate", defaultInt = 100) int sampleRate,
            @PluginElement("Filter") Filter filter) {
        if (StringUtil.isBlank(name)) {
            LOGGER.error("XxxLog4j2Appender name is required");
            return null;
        }
        if (StringUtil.isBlank(appName)) {
            LOGGER.error("XxxLog4j2Appender appName is required");
            return null;
        }
        try {
            LogSender sender = createLogSender(queueType, redisHost, redisPort, redisPassword, redisDatabase,
                    queueKey, rabbitmqHost, rabbitmqPort, rabbitmqUsername, rabbitmqPassword,
                    rabbitmqVirtualHost, rabbitmqQueue);
            String serverIp = resolveServerIp();
            return new XxxLog4j2Appender(name, filter, appName, env, sampleRate, sender, serverIp);
        } catch (Exception e) {
            LOGGER.error("Failed to create XxxLog4j2Appender", e);
            return null;
        }
    }

    private static LogSender createLogSender(String queueType,
                                             String redisHost, int redisPort, String redisPassword, int redisDatabase, String queueKey,
                                             String rabbitmqHost, int rabbitmqPort, String rabbitmqUsername, String rabbitmqPassword,
                                             String rabbitmqVirtualHost, String rabbitmqQueue) {
        QueueType type = parseQueueType(queueType);
        if (type == QueueType.RABBITMQ) {
            return new RabbitMqLogSender(rabbitmqHost, rabbitmqPort, rabbitmqUsername, rabbitmqPassword,
                    rabbitmqVirtualHost, rabbitmqQueue);
        }
        return new RedisLogSender(redisHost, redisPort, redisPassword, redisDatabase, queueKey);
    }

    private static QueueType parseQueueType(String value) {
        if (StringUtil.isBlank(value)) {
            return QueueType.REDIS;
        }
        try {
            return QueueType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return QueueType.REDIS;
        }
    }

    private static String resolveServerIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public void append(LogEvent event) {
        if (logSender == null) {
            return;
        }
        String level = event.getLevel().name();
        if (!samplingPolicy.shouldPass(level)) {
            return;
        }
        try {
            logSender.send(buildRecord(event));
        } catch (Exception e) {
            LOGGER.error("Failed to send log", e);
        }
    }

    @Override
    public void stop() {
        if (logSender != null) {
            logSender.close();
        }
        super.stop();
    }

    private LogRecord buildRecord(LogEvent event) {
        LogRecord record = new LogRecord();
        record.setAppName(appName);
        record.setEnv(env);
        record.setServerIp(serverIp);
        record.setThreadName(event.getThreadName());
        record.setLoggerName(event.getLoggerName());
        record.setLevel(event.getLevel().name());
        record.setMessage(event.getMessage().getFormattedMessage());
        record.setTimestamp(event.getTimeMillis());

        Map<String, String> context = event.getContextData().toMap();
        if (context != null && !context.isEmpty()) {
            record.setMdc(new HashMap<>(context));
            record.setTraceId(context.get(LogConstants.MDC_TRACE_ID));
            record.setSpanId(context.get(LogConstants.MDC_SPAN_ID));
            record.setParentSpanId(context.get(LogConstants.MDC_PARENT_SPAN_ID));
        }

        StackTraceElement source = event.getSource();
        if (source != null) {
            record.setClassName(source.getClassName());
            record.setMethodName(source.getMethodName());
        }

        Throwable thrown = event.getThrown();
        if (thrown != null) {
            StringWriter sw = new StringWriter();
            thrown.printStackTrace(new PrintWriter(sw));
            record.setStackTrace(sw.toString());
        }

        LogRecordIdResolver.ensureId(record);
        return record;
    }
}
