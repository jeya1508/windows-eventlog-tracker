package org.logging.producerconsumer;

import org.logging.entity.AlertProfile;
import org.logging.repository.CloseableRecordTracker;
import org.logging.service.ElasticSearchUtil;
import org.logging.service.EmailService;
import org.logging.service.LoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogConsumer implements Runnable {
    private final CircularBlockingQueue<Map<String, String>> queue;
    private final List<AlertProfile> alertProfiles;
    private final List<Map<String, String>> buffer;
    private final List<Map<String, String>> alertBuffer;
    private final Set<String> triggeredProfiles = new HashSet<>();
    private final ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();

    private static final int LOGS_BATCH_SIZE = 100;
    private static final int ALERT_BATCH_SIZE = 100;

    private final AtomicBoolean isDone;
    private final String deviceName;

    private static final Logger logger = LoggerFactory.getLogger(LogConsumer.class);

    public LogConsumer(CircularBlockingQueue<Map<String, String>> queue, List<AlertProfile> alertProfiles, String deviceName,AtomicBoolean isDone) {
        this.queue = queue;
        this.alertProfiles = alertProfiles;
        this.buffer = new ArrayList<>();
        this.alertBuffer = new ArrayList<>();
        this.isDone = isDone;
        this.deviceName = deviceName;
    }

    static CloseableRecordTracker recordTracker;

    static {
        try {
            recordTracker = CloseableRecordTracker.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        logger.info("Log consumer started. Waiting to process logs...");

        while (true) {
            List<Map<String, String>> logsToProcess = new ArrayList<>();

            while (logsToProcess.size() < LOGS_BATCH_SIZE) {
                try {
                    Map<String, String> log = queue.consume();
                    Thread.sleep(1000);
                    if (log != null) {
                        logsToProcess.add(log);
                        logger.debug("Size of logToProcess is {}",logsToProcess.size());
                        logger.info("Log added to process batch: {}", log);
                    } else {
                        if (isDone.get()) {
                            logger.info("Producer is done, no more logs to consume. Processing final batch...");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            logger.debug("Logs reached the Buffer size");
            logger.debug("Size of logToProcess is {}",logsToProcess.size());
            for (Map<String, String> log : logsToProcess) {
                try {
                    processLog(log);
                } catch (MessagingException e) {
                    logger.error("Messaging exception: {}", e.getMessage());
                }
            }
            if (!buffer.isEmpty()) {
                indexBufferedLogs();
            }
        }
    }

    private void processLog(Map<String, String> log) throws MessagingException {
        logger.debug("Into the process logs");
        buffer.add(log);
        logger.info("Buffer size is {}",buffer.size() + "and the device is "+deviceName);
        if (buffer.size() >= LOGS_BATCH_SIZE) {
            logger.info("Buffer reached {} logs. Indexing...", LOGS_BATCH_SIZE);
            indexBufferedLogs();
        }
        processAlerts(log);
    }

    public void finalizeProcessing() throws IOException {
        logger.info("Finalizing processing...");

        if (!buffer.isEmpty()) {
            logger.info("Finalizing and indexing remaining {} logs (windows-logs)...", buffer.size());

            if(deviceName == null) {
                logger.info("Indexing remaining {} logs to Elasticsearch (windows-logs)...", buffer.size());
                LoggingService.indexLogsToElasticSearch(buffer, "windows-logs");
            }
            else{
                logger.info("Indexing remaining {} logs ", buffer.size()+"to Elasticsearch (windows-logs-"+deviceName+")...");
                LoggingService.indexLogsToElasticSearch(buffer,"windows-logs-"+deviceName);
            }
            recordTracker.updateRecordNumber(Long.parseLong(buffer.getLast().get("record_number")));
            recordTracker.writeRecordNumberToFile();
            buffer.clear();
        }

        if (!alertBuffer.isEmpty()) {
            if(deviceName == null) {
                logger.info("Finalizing and indexing remaining {} alert logs (alerts-index)...", alertBuffer.size());
                LoggingService.indexLogsToElasticSearch(alertBuffer, "alerts-index");
            }
            else{
                logger.info("Finalizing and indexing remaining {}", alertBuffer.size()+" alert logs (alerts-index"+deviceName+")...");

                LoggingService.indexLogsToElasticSearch(alertBuffer,"alerts-index-"+deviceName);
            }
            alertBuffer.clear();
        }

        logger.info("Final processing complete.");
    }

    private void indexBufferedLogs() {
        if (!buffer.isEmpty()) {

            try {
                if(deviceName == null) {
                    logger.info("Indexing {} logs to Elasticsearch (windows-logs)...", buffer.size());
                    LoggingService.indexLogsToElasticSearch(buffer, "windows-logs");
                }
                else{
                    logger.info("Indexing {} logs ", buffer.size()+"to Elasticsearch (windows-logs-"+deviceName+")...");
                    LoggingService.indexLogsToElasticSearch(buffer,"windows-logs-"+deviceName);
                }
                recordTracker.updateRecordNumber(Long.parseLong(buffer.getLast().get("record_number")));
                recordTracker.writeRecordNumberToFile();
            } catch (Exception e) {
                logger.error("Failed to index logs: {}", e.getMessage());
            }

            String recNo = buffer.getLast().get("record_number");
            if (recNo != null) {
                recordTracker.updateRecordNumber(Long.parseLong(recNo));
                logger.info("Updated record tracker with record number: {}", recNo);
            }

            buffer.clear();
            logger.info("Buffer cleared after indexing.");
        }
    }

    private void processAlerts(Map<String, String> log) throws MessagingException {
        for (AlertProfile profile : alertProfiles) {
            if (elasticSearchUtil.logMatchesCriteria(log, profile.getCriteria())) {
                Map<String, String> alertLog = new HashMap<>(log);
                alertLog.put("profile_name", profile.getProfileName());
                alertBuffer.add(alertLog);

                if (!triggeredProfiles.contains(profile.getProfileName())) {
                    sendAlertEmail(profile);
                    triggeredProfiles.add(profile.getProfileName());
                }
            }
        }

        if (alertBuffer.size() >= ALERT_BATCH_SIZE) {
            logger.info("Alert buffer reached {} logs. Indexing alert logs...", ALERT_BATCH_SIZE);
            if(deviceName == null) {
                LoggingService.indexLogsToElasticSearch(alertBuffer, "alerts-index");
            }
            else{
                LoggingService.indexLogsToElasticSearch(alertBuffer,"alerts-index-"+deviceName);
            }
            alertBuffer.clear();
        }
    }

    private void sendAlertEmail(AlertProfile profile) throws MessagingException {
        String recipientEmail = profile.getNotifyEmail();
        if (recipientEmail != null && !recipientEmail.isEmpty()) {
            String emailSubject = "Alert Triggered: " + profile.getProfileName();
            String emailBody = "Alert was triggered for the profile " + profile.getProfileName() +
                    "\nTo view all alerts, visit http://localhost:4200/";
            EmailService.sendEmail(recipientEmail, emailSubject, emailBody);
            logger.info("Sent alert email to: {}", recipientEmail);
        }
    }
}
