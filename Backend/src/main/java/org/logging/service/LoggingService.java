package org.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.sun.jna.platform.win32.Advapi32Util;
import org.logging.config.ElasticSearchConfig;
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
    private static final int BATCH_SIZE = 1000;
    static ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
    public static void collectWindowsLogs(String name) {
        Set<String> triggeredProfiles = new HashSet<>();

        List<Map<String, Object>> buffer = new ArrayList<>();
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String username = System.getProperty("user.name");

            AlertProfileRepository alertProfileRepository = new AlertProfileRepository();
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
                        indexSingleLogToElasticSearch(alertLog, "alerts");

                        if (!triggeredProfiles.contains(profile.getProfileName())) {
                            String recipientEmail = profile.getNotifyEmail();
                            if (recipientEmail != null && !recipientEmail.isEmpty()) {
                                String emailSubject = "Alert Triggered: " + profile.getProfileName();
                                String emailBody = "Alert was triggered for the profile "+profile.getProfileName()+"\n To view all alerts, visit http://localhost:4200/";
                                EmailService.sendEmail(recipientEmail, emailSubject, emailBody);

                                triggeredProfiles.add(profile.getProfileName());
                            }
                        }
                    }
                }

                if (buffer.size() >= BATCH_SIZE) {
                    indexLogsToElasticSearch(buffer, "windows-event-logs");
                    buffer.clear();
                }
            }
            if (!buffer.isEmpty()) {
                indexLogsToElasticSearch(buffer, "windows-event-logs");
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
                IndexRequest<Map<String, Object>> indexRequest = IndexRequest.of(i -> i
                        .index(indexName)
                        .id(recordNumber)
                        .document(logData)
                );

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

            System.out.println("Indexed " + response.items().size() + " documents");
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
    public static void indexSingleLogToElasticSearch(Map<String, Object> log, String indexName) {
        ElasticsearchClient client = ElasticSearchConfig.createElasticsearchClient();
        try {
            String recordNumber = (String) log.get("record_number");
            IndexRequest<Map<String, Object>> indexRequest = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(recordNumber)
                    .document(log)
            );

            IndexResponse response = client.index(indexRequest);

            logger.info("Index {}",response.index());
        } catch (IOException e) {
            logger.error("Elasticsearch indexing failed for index: {}. Error: {}", indexName, e.getMessage());
        } finally {
            try {
                ElasticSearchConfig.closeClient();
            } catch (Exception e) {
                logger.error("Failed to close Elasticsearch client: {}", e.getMessage());
            }
        }
    }


}
