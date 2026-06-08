package com.xxxlog.server.config;

public class RabbitMqProperties {

    private String queue = "xxx-log.queue";
    private String deadLetterQueue = "xxx-log.dlq";

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public void setDeadLetterQueue(String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }
}
