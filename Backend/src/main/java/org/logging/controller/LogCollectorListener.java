package org.logging.controller;
import org.logging.service.LogCollectorTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class LogCollectorListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(LogCollectorListener.class);
    private Thread logThread;
    private LogCollectorTask logCollectorTask;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logCollectorTask = new LogCollectorTask();

        logThread = new Thread(logCollectorTask);
        logThread.start();
        System.out.println("Log collection started...");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (logCollectorTask != null) {
            logCollectorTask.stop();
        }
        try {
            if (logThread != null) {
                logThread.join();
            }
        } catch (InterruptedException e) {
           logger.error(e.getMessage());
        }
        System.out.println("Log collection stopped...");
    }
}
