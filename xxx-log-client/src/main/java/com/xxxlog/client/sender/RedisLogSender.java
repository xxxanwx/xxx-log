package com.xxxlog.client.sender;

import com.xxxlog.common.util.StringUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;

/**
 * 异步批量写入 Redis List 队列。
 */
public class RedisLogSender extends AbstractAsyncLogSender {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final String queueKey;

    public RedisLogSender(String host, int port, String password, int database, String queueKey) {
        super("xxx-log-redis-sender");
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withDatabase(database);
        if (StringUtil.isNotBlank(password)) {
            builder.withPassword(password.toCharArray());
        }
        this.redisClient = RedisClient.create(builder.build());
        this.connection = redisClient.connect();
        this.queueKey = queueKey;
    }

    @Override
    protected void pushBatch(List<String> batch) {
        RedisCommands<String, String> commands = connection.sync();
        commands.lpush(queueKey, batch.toArray(new String[0]));
    }

    @Override
    protected void doClose() {
        connection.close();
        redisClient.shutdown();
    }
}
