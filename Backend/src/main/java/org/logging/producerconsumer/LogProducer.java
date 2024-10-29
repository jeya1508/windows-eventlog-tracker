package org.logging.producerconsumer;

import org.logging.config.EventLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
public class LogProducer implements Runnable {
    private final CircularBlockingQueue<Map<String, String>> queue;
    private final EventLogCollector eventLogCollector;
    private final long lastRecordNumber;
    private final String ipAddress;
    private final String hostName;
    private final String password;
    private final AtomicBoolean isDone;

    private static final Logger logger = LoggerFactory.getLogger(LogProducer.class);

    public LogProducer(CircularBlockingQueue<Map<String, String>> queue, String ipAddress, String hostName, String password, long lastRecordNumber, AtomicBoolean isDone) {
        this.queue = queue;
        this.ipAddress = ipAddress;
        this.hostName = hostName;
        this.password = password;
        this.eventLogCollector = new EventLogCollector();
        this.lastRecordNumber = lastRecordNumber;
        this.isDone = isDone;
    }

    @Override
    public void run() {
        try {
            Map<String, String>[] logs = eventLogCollector.collectWindowsLogs(ipAddress, hostName, password, lastRecordNumber);
            logger.info("Size of event logs is {} ", (logs != null ? logs.length : 0));
            String hostName = InetAddress.getLocalHost().getHostName();
            String username = System.getProperty("user.name");
            if (logs != null) {
                for (Map<String, String> log : logs) {
                    try {
                        log.put("hostname",hostName);
                        log.put("username",username);
                        queue.produce(log);
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if(queue.getProducerSize()>0)
            {
                serializeRemainingLogs();
            }
            isDone.set(true);
            logger.info("LogProducer completed.");
        } catch (Exception e) {
            logger.error("Error in log production: {}", e.getMessage());
        }
    }


    private void serializeRemainingLogs() {
        if (queue.getProducerSize() > 0) {
            logger.info("Serializing remaining logs in producer queue before stopping.");
            queue.serializeProducerQueueToFile();
        }
    }
}
