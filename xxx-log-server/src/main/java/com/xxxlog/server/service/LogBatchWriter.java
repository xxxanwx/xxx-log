package com.xxxlog.server.service;

import com.xxxlog.common.model.LogRecord;
import com.xxxlog.common.util.JsonUtil;
import com.xxxlog.server.dingtalk.DingTalkAlertService;
import com.xxxlog.server.es.EsLogWriter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志写入 ES：消费端解析后直接 bulk 写入，不经过额外中间件。
 */
@Service
public class LogBatchWriter {

    private static final Logger log = LoggerFactory.getLogger(LogBatchWriter.class);

    private final EsLogWriter esLogWriter;
    private final ObjectProvider<DingTalkAlertService> dingTalkAlertService;
    private volatile boolean indexReady;

    public LogBatchWriter(EsLogWriter esLogWriter,
                          ObjectProvider<DingTalkAlertService> dingTalkAlertService) {
        this.esLogWriter = esLogWriter;
        this.dingTalkAlertService = dingTalkAlertService;
    }

    @PostConstruct
    public void initIndexTemplate() {
        esLogWriter.ensureIndexTemplate();
        indexReady = true;
        log.info("LogBatchWriter ready, ES index template ensured");
    }

    /**
     * 单条日志直接写入 ES（RabbitMQ 消费端）。
     */
    public void writeJson(String json) {
        writeBatch(json == null ? List.of() : List.of(json));
    }

    /**
     * 批量日志直接写入 ES（Redis 定时消费端）。
     */
    public void writeBatch(List<String> jsonList) {
        if (!indexReady || jsonList == null || jsonList.isEmpty()) {
            return;
        }
        List<LogRecord> records = new ArrayList<>(jsonList.size());
        for (String json : jsonList) {
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                LogRecord record = JsonUtil.fromJson(json, LogRecord.class);
                records.add(record);
                dingTalkAlertService.ifAvailable(service -> service.onLogIngested(record));
            } catch (Exception e) {
                log.warn("Invalid log message skipped: {}", e.getMessage());
            }
        }
        if (!records.isEmpty()) {
            esLogWriter.bulkWrite(records);
            log.info("Wrote {} log record(s) to ES", records.size());
        }
    }
}
