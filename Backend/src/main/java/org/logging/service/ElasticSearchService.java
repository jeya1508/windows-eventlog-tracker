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

    public ElasticSearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }
    public List<LogInfo> getAllLogs(int pageSize, String[] searchAfter, String sortBy, String sortOrder) throws Exception {

        String sortField = (sortBy != null && !sortBy.isEmpty()) ? getSortField(sortBy) : "time_generated";
        SortOrder sortingOrder = ("desc".equalsIgnoreCase(sortOrder) || sortOrder == null) ? SortOrder.Desc : SortOrder.Asc;

        SearchRequest searchRequest = SearchRequest.of(builder -> {
            builder.index("windows-event-logs")
                    .size(pageSize)
                    .sort(sort -> sort.field(f -> f.field(sortField).order(sortingOrder)))
                    .sort(sort -> sort.field(f -> f.field("_seq_no").order(sortingOrder)));

            if (searchAfter != null && searchAfter.length > 0) {
                List<FieldValue> searchAfterValues = Arrays.stream(searchAfter)
                        .map(val -> isNumeric(val) ? FieldValue.of(Long.parseLong(val)) : FieldValue.of(val))
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
        Query query = parseKqlToQuery(kqlQuery);
        SearchRequest searchRequest = SearchRequest.of(builder -> {
            builder.index("windows-event-logs")
                    .query(query)
                    .size(pageSize)
                    .sort(sort -> sort.field(f -> f.field("time_generated").order(SortOrder.Desc)));

            if (searchAfter != null && searchAfter.length > 0) {
                List<FieldValue> searchAfterValues = Arrays.stream(searchAfter)
                        .map(val -> isNumeric(val) ? FieldValue.of(Long.parseLong(val)) : FieldValue.of(val))
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

    public long getTotalRecords() throws Exception {
        CountRequest countRequest = CountRequest.of(c -> c.index("windows-event-logs"));
        CountResponse countResponse = elasticsearchClient.count(countRequest);
        return countResponse.count();
    }

    public long getSearchedCount() {
        return SEARCH_COUNT;
    }
    private String getSortField(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "event_id":
                return "event_id";
            case "username":
                return "username.keyword";
            case "hostname":
                return "hostname.keyword";
            case "time_generated":
            default:
                return "time_generated";
        }
    }
    private boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }
}