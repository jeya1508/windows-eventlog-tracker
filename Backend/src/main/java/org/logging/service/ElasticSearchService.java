package org.logging.service;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import org.logging.entity.DeviceInfo;
import org.logging.entity.LogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class ElasticSearchService {
    public long SEARCH_COUNT = 0;

    private final ElasticsearchClient elasticsearchClient;
    private final ValidationService validationService;
    ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);
    public ElasticSearchService(ElasticsearchClient elasticsearchClient, ValidationService validationService) {
        this.elasticsearchClient = elasticsearchClient;
        this.validationService = validationService;
    }
    public List<LogInfo> getAllLogs(DeviceInfo deviceInfo, int pageSize, String[] searchAfter, String sortBy, String sortOrder) throws Exception {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? elasticSearchUtil.getSortField(sortBy) : "time_generated";
        SortOrder sortingOrder = ("desc".equalsIgnoreCase(sortOrder) || sortOrder == null) ? SortOrder.Desc : SortOrder.Asc;
        String deviceName = (deviceInfo != null) ? deviceInfo.getDeviceName() : null;
        String indexName = (deviceName == null) ? "windows-logs" : "windows-logs-" + deviceName;

        if (!isIndexExists(indexName)) {
            logger.error("Index does not exist: {}", indexName);
            return Collections.emptyList(); // Handle missing index gracefully
        }

        logger.info("Preparing search for index: {}, sortField: {}, sortOrder: {}, pageSize: {}", indexName, sortField, sortingOrder, pageSize);

        SearchRequest searchRequest = SearchRequest.of(builder -> {
            builder.index(indexName)
                    .size(pageSize)
                    .sort(sort -> sort.field(f -> f.field(sortField).order(sortingOrder)))
                    .sort(sort -> sort.field(f -> f.field("_seq_no").order(sortingOrder)));

            if (searchAfter != null && searchAfter.length > 0) {
                List<FieldValue> searchAfterValues = Arrays.stream(searchAfter)
                        .map(val -> validationService.isNumeric(val) ? FieldValue.of(Long.parseLong(val)) : FieldValue.of(val))
                        .collect(Collectors.toList());
                builder.searchAfter(searchAfterValues);
            }

            return builder;
        });

        try {
            SearchResponse<LogInfo> searchResponse = elasticsearchClient.search(searchRequest, LogInfo.class);
            if (searchResponse == null || searchResponse.hits().hits().isEmpty()) {
                logger.warn("No hits found for index: {}", indexName);
                return Collections.emptyList();
            }

            return searchResponse.hits().hits().stream()
                    .map(hit -> {
                        LogInfo logInfo = hit.source();
                        if (logInfo != null) {
                            logInfo.setSortValues(Arrays.stream(hit.sort().toArray())
                                    .map(fieldValue -> (fieldValue instanceof FieldValue) ? ((FieldValue) fieldValue)._get() : fieldValue)
                                    .toArray());
                        }
                        return logInfo;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error executing search for index {}: {}", indexName, e.getMessage());
            throw e;
        }
    }


    public boolean isIndexExists(String indexName) {
        boolean exists = false;
        try {
            GetIndexRequest request = new GetIndexRequest.Builder()
                    .index(indexName)
                    .build();

            elasticsearchClient.indices().get(request);
            exists = true;
        } catch (ElasticsearchException e) {
            if (e.response().status() != 404) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exists;
    }

    public List<LogInfo> searchLogs(DeviceInfo deviceInfo, String kqlQuery, int pageSize, String[] searchAfter) throws Exception {
        Query query = elasticSearchUtil.parseKqlToQuery(kqlQuery);
        String deviceName = (deviceInfo!=null) ? deviceInfo.getDeviceName() : null;
        String indexName = (deviceName == null )? "windows-logs" : "windows-logs-"+deviceName;
        if(isIndexExists(indexName)) {
            SearchRequest searchRequest = SearchRequest.of(builder -> {
                builder.index("windows-logs")
                        .query(query)
                        .size(pageSize)
                        .sort(sort -> sort.field(f -> f.field("time_generated").order(SortOrder.Desc)));

                if (searchAfter != null && searchAfter.length > 0) {
                    List<FieldValue> searchAfterValues = Arrays.stream(searchAfter)
                            .map(val -> validationService.isNumeric(val) ? FieldValue.of(Long.parseLong(val)) : FieldValue.of(val))
                            .collect(Collectors.toList());

                    builder.searchAfter(searchAfterValues);
                }

                return builder;
            });

            SearchResponse<LogInfo> searchResponse = elasticsearchClient.search(searchRequest, LogInfo.class);
            SEARCH_COUNT = searchResponse.hits().total().value();
            return searchResponse.hits().hits().stream()
                    .map(hit -> {
                        LogInfo logInfo = hit.source();
                        if (logInfo != null) {
                            Object[] sortValues = Arrays.stream(hit.sort().toArray())
                                    .map(fieldValue -> {
                                        if (fieldValue instanceof FieldValue) {
                                            return ((FieldValue) fieldValue)._get();
                                        }
                                        return fieldValue;
                                    }).toArray();
                            logInfo.setSortValues(sortValues);
                        }
                        return logInfo;
                    })
                    .collect(Collectors.toList());
        }
        else{
            return null;
        }
    }
    public long getSearchedCount() {
        return SEARCH_COUNT;
    }

}