package org.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.logging.entity.AlertInfo;
import org.logging.entity.AlertProfile;
import org.logging.repository.AlertProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AlertRetrievalService {

    public long SEARCH_COUNT = 0;
    private final ElasticsearchClient elasticsearchClient;
    private final ValidationService validationService;

    private static final Logger logger = LoggerFactory.getLogger(AlertRetrievalService.class);
    ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
    public AlertRetrievalService(ElasticsearchClient elasticsearchClient,ValidationService validationService) {
        this.elasticsearchClient = elasticsearchClient;
        this.validationService = validationService;
    }

    public List<AlertInfo> getAllAlerts(int pageSize, String[] searchAfter) throws IOException {

        SearchRequest searchRequest = SearchRequest.of(builder -> {
            builder.index("alerts-index")
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
            Query query = elasticSearchUtil.parseKqlToQuery(kqlQuery);
            SearchRequest searchRequest = SearchRequest.of(builder -> {
                builder.index("alerts-index")
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
    public long getSearchedCount() {
        return SEARCH_COUNT;
    }

    public List<String> getAllProfiles() {
        List<String> profileNameList = new ArrayList<>();
        AlertProfileRepository alertProfileRepository = new AlertProfileRepository();
        List<AlertProfile> profileList = alertProfileRepository.findAll();
        for(AlertProfile alertProfile:profileList)
        {
            profileNameList.add(alertProfile.getProfileName());
        }
        return profileNameList;
    }

    public List<AlertInfo> getAlerts(String kqlQuery) throws Exception {
        int pageSize = 1000;
        int maxRecords = 50000;
        List<AlertInfo> allAlerts = new ArrayList<>();
        String[] searchAfter = null;

        while (true) {
            List<AlertInfo> alerts;

            if (kqlQuery != null && !kqlQuery.isEmpty()) {
                alerts = searchAlerts(kqlQuery, pageSize, searchAfter);
            } else {
                alerts = getAllAlerts(pageSize, searchAfter);
            }
            logger.info("Fetched {} alerts", alerts.size());

            if (alerts.isEmpty()) {
                logger.info("No more alerts found");
                break;
            }

            allAlerts.addAll(alerts);
            logger.info("Total records fetched {}", allAlerts.size());

            if (allAlerts.size() >= maxRecords) {
                logger.info("Reached maximum record of {}", maxRecords);
                break;
            }

            if (alerts.size() < pageSize) {
                logger.info("Alerts less than page size");
                break;
            }
            AlertInfo lastAlert = alerts.getLast();
            Object[] sortValues = lastAlert.getSortValues();
            searchAfter = new String[sortValues.length];

            for (int i = 0; i < sortValues.length; i++) {
                searchAfter[i] = String.valueOf(sortValues[i]);
            }
            logger.info("Search after value is {}", searchAfter[0]);
        }

        return allAlerts;
    }

}
