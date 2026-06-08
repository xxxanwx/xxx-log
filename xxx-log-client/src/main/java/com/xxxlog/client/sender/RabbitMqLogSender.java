package com.xxxlog.client.sender;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.xxxlog.common.util.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 异步写入 RabbitMQ 队列（默认交换机 direct 到队列名）。
 */
public class RabbitMqLogSender extends AbstractAsyncLogSender {

    private final Connection connection;
    private final Channel channel;
    private final String queueName;

    public RabbitMqLogSender(String host,
                             int port,
                             String username,
                             String password,
                             String virtualHost,
                             String queueName) {
        super("xxx-log-rabbitmq-sender");
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setVirtualHost(StringUtil.isBlank(virtualHost) ? "/" : virtualHost);
            if (StringUtil.isNotBlank(username)) {
                factory.setUsername(username);
            }
            if (StringUtil.isNotBlank(password)) {
                factory.setPassword(password);
            }
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            this.queueName = queueName;
            channel.queueDeclare(queueName, true, false, false, null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect RabbitMQ", e);
        }
    }

    @Override
    protected void pushBatch(List<String> batch) throws Exception {
        for (String json : batch) {
            channel.basicPublish("", queueName, null, json.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void doClose() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }
}
