package org.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.sun.jna.platform.win32.Advapi32Util;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.logging.entity.AlertProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.logging.repository.AlertProfileRepository;
import static org.logging.repository.ElasticSearchRepository.getLastIndexedRecordNumber;

public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final int BATCH_SIZE = 1000;

    public static void collectWindowsLogs(String name) {
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
                    if (logMatchesCriteria(logData, profile.getCriteria())) {

                        Map<String, Object> alertLog = new HashMap<>(logData);
                        alertLog.put("profile_name", profile.getProfileName());
                        indexSingleLogToElasticSearch(alertLog, "alerts");
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

    public static RestClient establishESConnection(){
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "elastic"));

        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setDefaultRequestConfig(RequestConfig.custom()
                                .setConnectTimeout(120000)
                                .setSocketTimeout(120000)
                                .build()));
        return builder.build();

    }
    public static void indexLogsToElasticSearch(List<Map<String, Object>> logs, String indexName) {
        RestClient restClient = establishESConnection();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);

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
                restClient.close();
            } catch (IOException e) {
                logger.error("I/O exception {}",e.getMessage());
            }
        }
    }
    public static void indexSingleLogToElasticSearch(Map<String, Object> log, String indexName) {
        RestClient restClient = establishESConnection();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);
        try {
            String recordNumber = (String) log.get("record_number");
            IndexRequest<Map<String, Object>> indexRequest = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(recordNumber)
                    .document(log)
            );

            IndexResponse response = client.index(indexRequest);

//            logger.info("Index {}",response.index());
        } catch (IOException e) {
            logger.error("Elasticsearch indexing failed for index: {}. Error: {}", indexName, e.getMessage());
        } finally {
            try {
                restClient.close();
            } catch (IOException e) {
                logger.error("Failed to close Elasticsearch client: {}", e.getMessage());
            }
        }
    }
    public static boolean logMatchesCriteria(Map<String, Object> logData, String criteria) {
        String[] keyValue = criteria.split("=");
        if (keyValue.length == 2) {
            String key = keyValue[0].trim(); // Trim whitespace
            String value = keyValue[1].trim(); // Trim whitespace

            // Check if the logData contains the key
            if (logData.containsKey(key)) {
                // Get the log value and convert it to String for comparison
                String logValue = logData.get(key).toString().trim(); // Normalize the log value

                // Case-insensitive comparison
                return logValue.equalsIgnoreCase(value);
            }
        }
        return false;
    }


}
