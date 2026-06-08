package com.xxxlog.client.sender;

import com.xxxlog.common.model.LogRecord;
import com.xxxlog.common.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 异步缓冲发送基类，业务线程只入队不阻塞。
 */
abstract class AbstractAsyncLogSender implements LogSender {

    private static final int BATCH_SIZE = 50;
    private static final int QUEUE_CAPACITY = 10000;

    private final ArrayBlockingQueue<String> buffer;
    private final ExecutorService worker;
    private volatile boolean running = true;

    protected AbstractAsyncLogSender(String threadName) {
        this.buffer = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
        this.worker.submit(new Runnable() {
            @Override
            public void run() {
                flushLoop();
            }
        });
    }

    @Override
    public void send(LogRecord record) {
        if (!running) {
            return;
        }
        String json = JsonUtil.toJson(record);
        if (!buffer.offer(json)) {
            buffer.poll();
            buffer.offer(json);
        }
    }

    private void flushLoop() {
        while (running) {
            try {
                String first = buffer.poll(500, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                List<String> batch = new ArrayList<String>();
                batch.add(first);
                buffer.drainTo(batch, BATCH_SIZE - 1);
                pushBatch(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
                // 发送失败时不影响业务，下一轮重试
            }
        }
        flushRemaining();
    }

    private void flushRemaining() {
        List<String> batch = new ArrayList<String>();
        buffer.drainTo(batch);
        if (!batch.isEmpty()) {
            try {
                pushBatch(batch);
            } catch (Exception ignored) {
                // ignore on shutdown
            }
        }
    }

    protected abstract void pushBatch(List<String> batch) throws Exception;

    @Override
    public void close() {
        running = false;
        worker.shutdown();
        try {
            worker.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        doClose();
    }

    protected void doClose() {
        // optional hook
    }
}
