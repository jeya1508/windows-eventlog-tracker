package org.logging.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertScheduler {

    private final AlertSchedulerService alertSchedulerService;
    private static final Logger logger = LoggerFactory.getLogger(AlertScheduler.class);

    public AlertScheduler(AlertSchedulerService alertSchedulerService) {
        this.alertSchedulerService = alertSchedulerService;
    }

    public void start() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                alertSchedulerService.checkAndTriggerAlerts();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }, 0, 30, TimeUnit.MINUTES);
    }
}
