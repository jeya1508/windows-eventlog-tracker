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
            Query query = parseKqlToQuery(kqlQuery);
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

    private Query parseKqlToQuery(String kqlQuery) {
        if (kqlQuery.contains("=")) {
            String[] parts = kqlQuery.split("=");
            String field = parts[0].trim();
            String value = parts[1].trim();

            String fieldType = getFieldType(field);

            switch (fieldType) {
                case "keyword":
                    return Query.of(q -> q
                            .term(t -> t
                                    .field(field + ".keyword")
                                    .value(v -> v.stringValue(value))));
                case "integer":
                    return Query.of(q -> q
                            .term(t -> t
                                    .field(field)
                                    .value(Integer.parseInt(value))));

                case "date":
                    long epochMillis = Long.parseLong(value) * 1000;
                    return Query.of(q -> q
                            .term(t -> t
                                    .field(field)
                                    .value(epochMillis)));
                default:
                    return null;
            }
        }
        return null;
    }

    private String getFieldType(String field) {
        if(field.equals("event_id") || field.equals("event_category"))
        {
            return "integer";
        }
        else if (field.equals("time_generated") || field.equals("time_written"))
        {
            return "date";

        }
        else {
            return "keyword";
        }
    }
    public long getSearchedCount() {
        return SEARCH_COUNT;
    }
}
