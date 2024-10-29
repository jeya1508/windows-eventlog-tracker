package org.logging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogCollectorTask implements Runnable {
    private volatile boolean running = true;
    private static final Logger logger = LoggerFactory.getLogger(LogCollectorTask.class);
    private final ScheduledExecutorService scheduler;

    public LogCollectorTask() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void run() {
        scheduler.scheduleAtFixedRate(() -> {
            if (running) {
                try {
                    LoggingService.collectWindowsLogs(null,null,null);
                } catch (Exception e) {
                    logger.error("Error collecting logs: {}", e.getMessage());
                }
            }
        }, 0, 15, TimeUnit.MINUTES);
    }

    public void stop() {
        this.running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
