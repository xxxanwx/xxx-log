package com.xxxlog.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = RabbitAutoConfiguration.class)
@EnableScheduling
@EnableAsync
public class XxxLogServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxxLogServerApplication.class, args);
    }
}
