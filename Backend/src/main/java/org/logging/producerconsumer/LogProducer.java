package org.logging.producerconsumer;

import org.logging.config.EventLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
public class LogProducer implements Runnable {
    private final CircularBlockingQueue<Map<String, String>> queue;
    private final EventLogCollector eventLogCollector;
    private final long lastRecordNumber;
    private final AtomicBoolean isDone;

    private static final Logger logger = LoggerFactory.getLogger(LogProducer.class);

    public LogProducer(CircularBlockingQueue<Map<String, String>> queue, long lastRecordNumber, AtomicBoolean isDone) {
        this.queue = queue;
        this.eventLogCollector = new EventLogCollector();
        this.lastRecordNumber = lastRecordNumber;
        this.isDone = isDone;
    }

    @Override
    public void run() {
        try {
            Map<String, String>[] logs = eventLogCollector.collectWindowsLogs(lastRecordNumber);
            logger.info("Size of event logs is {} ", (logs != null ? logs.length : 0));

            if (logs != null) {
                for (Map<String, String> log : logs) {
                    try {
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
