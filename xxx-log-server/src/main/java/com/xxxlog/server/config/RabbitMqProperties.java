package com.xxxlog.server.config;

public class RabbitMqProperties {

    private String queue = "xxx-log.queue";

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
}
