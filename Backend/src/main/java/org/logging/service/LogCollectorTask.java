package org.logging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCollectorTask implements Runnable {
    private volatile boolean running = true;
    private static final Logger logger = LoggerFactory.getLogger(LogCollectorTask.class);
    @Override
    public void run() {
        while (running) {
            try {
//                System.out.println("In the running loop");
                LoggingService.collectWindowsLogs("Application");
//                LoggingService.collectWindowsLogs("System");

            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    public void stop() {
        this.running = false;
    }
}
