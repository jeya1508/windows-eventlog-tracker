package org.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.logging.entity.AlertInfo;
import org.logging.exception.ValidationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AlertRetrievalService {

    public long SEARCH_COUNT = 0;
    private final ElasticsearchClient elasticsearchClient;
    private final ValidationService validationService;
    ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
    public AlertRetrievalService(ElasticsearchClient elasticsearchClient,ValidationService validationService) {
        this.elasticsearchClient = elasticsearchClient;
        this.validationService = validationService;
    }

    public List<AlertInfo> getAllAlerts(int pageSize, String[] searchAfter) throws IOException {

        SearchRequest searchRequest = SearchRequest.of(builder -> {
            builder.index("alerts")
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

        SearchResponse<AlertInfo> searchResponse = elasticsearchClient.search(searchRequest, AlertInfo.class);

        return searchResponse.hits().hits().stream()
                .map(hit -> {
                    AlertInfo logInfo = hit.source();
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
    public List<AlertInfo> searchAlerts(String kqlQuery, int pageSize, String[] searchAfter) throws Exception {
        if(validationService.isValidCriteria(kqlQuery)) {
            Query query = elasticSearchUtil.parseKqlToQuery(kqlQuery);
            SearchRequest searchRequest = SearchRequest.of(builder -> {
                builder.index("alerts")
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

            SearchResponse<AlertInfo> searchResponse = elasticsearchClient.search(searchRequest, AlertInfo.class);
            SEARCH_COUNT = searchResponse.hits().total().value();
            return searchResponse.hits().hits().stream()
                    .map(hit -> {
                        AlertInfo logInfo = hit.source();
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
            throw new ValidationException("Criteria constraints not matched");
        }
    }
    public long getSearchedCount() {
        return SEARCH_COUNT;
    }
}
