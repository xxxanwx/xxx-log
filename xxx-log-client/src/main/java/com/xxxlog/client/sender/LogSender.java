package com.xxxlog.client.sender;

import com.xxxlog.common.model.LogRecord;

public interface LogSender {

    void send(LogRecord record);

    void close();
}
