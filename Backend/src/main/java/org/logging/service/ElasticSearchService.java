package org.logging.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.Arrays;
import java.util.List;
import org.logging.entity.LogInfo;

import java.util.stream.Collectors;

public class ElasticSearchService {
    public long SEARCH_COUNT = 0;

    private final ElasticsearchClient elasticsearchClient;
    private final ValidationService validationService;
    ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
    public ElasticSearchService(ElasticsearchClient elasticsearchClient, ValidationService validationService) {
        this.elasticsearchClient = elasticsearchClient;
        this.validationService = validationService;
    }
    public List<LogInfo> getAllLogs(int pageSize, String[] searchAfter, String sortBy, String sortOrder) throws Exception {

        String sortField = (sortBy != null && !sortBy.isEmpty()) ? elasticSearchUtil.getSortField(sortBy) : "time_generated";
        SortOrder sortingOrder = ("desc".equalsIgnoreCase(sortOrder) || sortOrder == null) ? SortOrder.Desc : SortOrder.Asc;

        SearchRequest searchRequest = SearchRequest.of(builder -> {
            builder.index("windows-event-logs")
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

        SearchResponse<LogInfo> searchResponse = elasticsearchClient.search(searchRequest, LogInfo.class);

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



    public List<LogInfo> searchLogs(String kqlQuery, int pageSize, String[] searchAfter) throws Exception {
        Query query = elasticSearchUtil.parseKqlToQuery(kqlQuery);
        SearchRequest searchRequest = SearchRequest.of(builder -> {
            builder.index("windows-event-logs")
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


    public long getSearchedCount() {
        return SEARCH_COUNT;
    }

}