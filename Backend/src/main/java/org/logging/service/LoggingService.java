package org.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.sun.jna.platform.win32.Advapi32Util;
import org.logging.config.ElasticSearchConfig;
import org.logging.entity.AlertProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.*;

import org.logging.repository.AlertProfileRepository;
import static org.logging.repository.ElasticSearchRepository.getLastIndexedRecordNumber;

public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final int LOGS_BATCH_SIZE = 2000;
    private static final int ALERT_BATCH_SIZE = 1000;
    private static final ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
    private static final AlertProfileRepository alertProfileRepository = new AlertProfileRepository();

    public static void collectWindowsLogs(String name) {
        Set<String> triggeredProfiles = new HashSet<>();

        List<Map<String, Object>> buffer = new ArrayList<>();
        List<Map<String, Object>> alertBuffer = new ArrayList<>();

        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String username = System.getProperty("user.name");

            List<AlertProfile> alertProfiles = alertProfileRepository.findAll();
            String lastIndexedRecordNumber = getLastIndexedRecordNumber();

            Advapi32Util.EventLogIterator it = new Advapi32Util.EventLogIterator(name);
            while (it.hasNext()) {
                Advapi32Util.EventLogRecord record = it.next();
                String currentRecordNumber = record.getRecord().RecordNumber.toString();

                if (lastIndexedRecordNumber != null && Long.parseLong(currentRecordNumber) <= Long.parseLong(lastIndexedRecordNumber)) {
                    continue;
                }

                Map<String, Object> logData = new HashMap<>();
                logData.put("event_id", record.getRecord().EventID.toString());
                logData.put("source", record.getSource());
                logData.put("event_type", record.getType());
                logData.put("record_number", currentRecordNumber);
                logData.put("event_category", record.getRecord().EventCategory.toString());
                logData.put("hostname", hostName);
                logData.put("username", username);
                logData.put("time_generated", record.getRecord().TimeGenerated.toString());
                logData.put("time_written", record.getRecord().TimeWritten.toString());

                buffer.add(logData);

                for (AlertProfile profile : alertProfiles) {
                    if (elasticSearchUtil.logMatchesCriteria(logData, profile.getCriteria())) {
                        Map<String, Object> alertLog = new HashMap<>(logData);
                        alertLog.put("profile_name", profile.getProfileName());
                        alertBuffer.add(alertLog);

                        if (!triggeredProfiles.contains(profile.getProfileName())) {
                            String recipientEmail = profile.getNotifyEmail();
                            if (recipientEmail != null && !recipientEmail.isEmpty()) {
                                String emailSubject = "Alert Triggered: " + profile.getProfileName();
                                String emailBody = "Alert was triggered for the profile " + profile.getProfileName() + "\n To view all alerts, visit http://localhost:4200/";
                                EmailService.sendEmail(recipientEmail, emailSubject, emailBody);

                                triggeredProfiles.add(profile.getProfileName());
                            }
                        }
                    }
                }

                if (buffer.size() >= LOGS_BATCH_SIZE) {
                    indexLogsToElasticSearch(buffer, "windows-event-logs");
                    buffer.clear();
                }
                if (alertBuffer.size() >= ALERT_BATCH_SIZE) {
                    indexLogsToElasticSearch(alertBuffer, "alerts");
                    alertBuffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                indexLogsToElasticSearch(buffer, "windows-event-logs");
            }

            if (!alertBuffer.isEmpty()) {
                indexLogsToElasticSearch(alertBuffer, "alerts");
            }

        } catch (Exception e) {
            logger.error("Error in collecting logs {}", e.getMessage());
        }
    }

    public static void indexLogsToElasticSearch(List<Map<String, Object>> logs, String indexName) {
        ElasticsearchClient client = ElasticSearchConfig.createElasticsearchClient();
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Map<String, Object> logData : logs) {
                String recordNumber = (String) logData.get("record_number");


                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(recordNumber)
                                .document(logData)
                        )
                );
            }

            BulkRequest bulkRequest = bulkBuilder.build();
            BulkResponse response = client.bulk(bulkRequest);

            logger.info("Indexed {} ",response.items().size() +" documents");
        } catch (IOException e) {
            logger.error("Error in ES {}",e.getMessage());
        } finally {
            try {
                ElasticSearchConfig.closeClient();
            } catch (Exception e) {
                logger.error("Exception while closing ES Client {}",e.getMessage());
            }
        }
    }
}
