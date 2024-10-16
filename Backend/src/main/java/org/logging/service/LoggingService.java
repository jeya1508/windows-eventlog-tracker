package org.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import org.logging.config.ElasticSearchConfig;
import org.logging.config.EventLogCollector;
import org.logging.entity.AlertProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

import org.logging.repository.AlertProfileRepository;
import static org.logging.repository.ElasticSearchRepository.getLastIndexedRecordNumber;

public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final int LOGS_BATCH_SIZE = 2000;
    private static final int ALERT_BATCH_SIZE = 1000;

    private static final EventLogCollector eventLogCollector = new EventLogCollector();
    private static final ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
    private static final AlertProfileRepository alertProfileRepository = new AlertProfileRepository();

    public static void collectWindowsLogs() {
        Set<String> triggeredProfiles = new HashSet<>();

        List<Map<String, String>> buffer = new ArrayList<>();
        List<Map<String, String>> alertBuffer = new ArrayList<>();

        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String username = System.getProperty("user.name");

            List<AlertProfile> alertProfiles = alertProfileRepository.findAll();
            String lastIndexedRecordNumber = getLastIndexedRecordNumber();
            long lastRecordNumber = (lastIndexedRecordNumber!=null)?Long.parseLong(lastIndexedRecordNumber):-1;
            Map<String, String>[] logs = eventLogCollector.collectWindowsLogs(lastRecordNumber);
            if(logs == null)
            {
                return;
            }
            for (Map<String, String> logData : logs) {
                logData.put("hostname",hostName);
                logData.put("username",username);

                buffer.add(logData);

                for (AlertProfile profile : alertProfiles) {
                    if (elasticSearchUtil.logMatchesCriteria(logData, profile.getCriteria())) {
                        Map<String, String> alertLog = new HashMap<>(logData);
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
                    indexLogsToElasticSearch(buffer, "windows-logs");
                    buffer.clear();
                }
                if (alertBuffer.size() >= ALERT_BATCH_SIZE) {
                    indexLogsToElasticSearch(alertBuffer, "alerts-index");
                    alertBuffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                indexLogsToElasticSearch(buffer, "windows-logs");
            }

            if (!alertBuffer.isEmpty()) {
                indexLogsToElasticSearch(alertBuffer, "alerts-index");
            }

        } catch (Exception e) {
            logger.error("Error in collecting logs {}", e.getMessage());
        }
    }

    public static void indexLogsToElasticSearch(List<Map<String, String>> logs, String indexName) {
        ElasticsearchClient client = ElasticSearchConfig.createElasticsearchClient();
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Map<String, String> logData : logs) {
                String recordNumber = logData.get("record_number");
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

            logger.info("Indexed {} ",response.items().size() +" documents in "+indexName);
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
