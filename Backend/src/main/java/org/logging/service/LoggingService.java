package org.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import org.logging.config.ElasticSearchConfig;
import org.logging.entity.AlertProfile;
import org.logging.entity.DeviceInfo;
import org.logging.producerconsumer.CircularBlockingQueue;
import org.logging.producerconsumer.LogConsumer;
import org.logging.producerconsumer.LogProducer;
import org.logging.repository.CloseableRecordTracker;
import org.logging.repository.ElasticSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.logging.repository.AlertProfileRepository;

public class LoggingService implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final int QUEUE_CAPACITY = 50;
    private static final int NUM_PRODUCERS = 1;
    private static final int NUM_CONSUMERS = 2;

    private static final AlertProfileRepository alertProfileRepository = new AlertProfileRepository();
    private static List<LogProducer> logProducers = new ArrayList<>();

    static CloseableRecordTracker recordTracker;

    static {
        try {
            recordTracker = CloseableRecordTracker.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void collectWindowsLogs(DeviceInfo deviceInfo) throws IOException {

        CircularBlockingQueue<Map<String, String>> queue = new CircularBlockingQueue<>(QUEUE_CAPACITY);

        String deviceName = (deviceInfo == null) ? null: deviceInfo.getDeviceName();
        String ipAddress = (deviceInfo == null) ? null: deviceInfo.getIpAddress();
        String userName = (deviceInfo == null) ? null : deviceInfo.getUserName();
        String password = (deviceInfo == null) ? null :deviceInfo.getPassword();

        recordTracker.updateRecordNumber(recordTracker.initializeRecordNumber(deviceName));

        long lastRecordNumber = recordTracker.getCurrentRecordNumber();
        if(lastRecordNumber==-1)
        {
            logger.info("It seems there is no record number in the file. Checking in ES");
            String recNo = ElasticSearchRepository.getLastIndexedRecordNumber(deviceName);
            lastRecordNumber = (recNo == null)? -1 : Long.parseLong(recNo);

        }
        recordTracker.updateRecordNumber(lastRecordNumber);
        List<AlertProfile> alertProfiles = alertProfileRepository.findAll();

        AtomicBoolean isDone = new AtomicBoolean(false);

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);

        for (int i = 0; i < NUM_PRODUCERS; i++) {
            LogProducer logProducer = new LogProducer(queue, ipAddress,userName,password, lastRecordNumber,isDone);
            logProducers.add(logProducer);
            executorService.submit(logProducer);

            logger.info("Producer thread started");
        }

        for (int i = 0; i < NUM_CONSUMERS; i++) {
            LogConsumer logConsumer = new LogConsumer(queue,alertProfiles,deviceName,isDone);
            executorService.submit(logConsumer);
        }
        executorService.shutdown();
        synchronized (LoggingService.class) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown Hook triggered. Closing LoggingService");

                try {
                    recordTracker.close();
                    if(!executorService.isShutdown()) {
                        executorService.shutdown();
                    }

                } catch (IOException e) {
                    logger.error("Error while shutdown {}", e.getMessage());
                }

            }));
        }
    }

    public static void indexLogsToElasticSearch(List<Map<String, String>> logs, String indexName) {
        ElasticsearchClient client = ElasticSearchConfig.createElasticsearchClient();
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Map<String, String> logData : logs) {
                Map<String, Object> transformedLogData = new HashMap<>(logData);

                transformedLogData.put("event_category", Integer.parseInt(logData.get("event_category")));
                transformedLogData.put("event_id", Integer.parseInt(logData.get("event_id")));

                long timeGeneratedMillis = Long.parseLong(logData.get("time_generated"));
                long timeWrittenMillis = Long.parseLong(logData.get("time_written"));
                transformedLogData.put("time_generated", timeGeneratedMillis);
                transformedLogData.put("time_written", timeWrittenMillis);

                String recordNumber = logData.get("record_number");
                logger.debug("Transformed log is {}",transformedLogData);
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(recordNumber)
                                .document(transformedLogData)
                        )
                );
            }

            BulkRequest bulkRequest = bulkBuilder.build();
            BulkResponse response = client.bulk(bulkRequest);

            logger.info("Indexed {} documents in {}", response.items().size(), indexName);
        } catch (IOException e) {
            logger.error("Error in ES {}", e.getMessage() + " in " + indexName);
        } finally {
            try {
                ElasticSearchConfig.closeClient();
            } catch (Exception e) {
                logger.error("Exception while closing ES Client {}", e.getMessage());
            }
        }
    }


    @Override
    public void close() throws IOException {
        recordTracker.close();
    }
}
