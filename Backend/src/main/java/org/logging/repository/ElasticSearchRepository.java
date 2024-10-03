package org.logging.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.*;
import org.logging.config.ElasticSearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ElasticSearchRepository {
    private static final ElasticsearchClient client = ElasticSearchConfig.createElasticsearchClient();
    private static  final Logger logger = LoggerFactory.getLogger(ElasticSearchRepository.class);
    public static String getLastIndexedRecordNumber() {
        try {
            SearchResponse<Map> searchResponse = client.search(s -> s
                            .index("windows-event-logs")
                            .sort(sort -> sort.field(f -> f.field("record_number.keyword").order(SortOrder.Desc)))  // Sort by record_number in descending order
                            .size(1),
                    Map.class
            );

            if (!searchResponse.hits().hits().isEmpty()) {
                Map<String, Object> source = searchResponse.hits().hits().get(0).source();
                return (String) source.get("record_number");
            }
        } catch (IOException e) {
            logger.error("Elasticsearch search failed", e);
        } catch (ElasticsearchException e) {
            logger.error("Elasticsearch exception:{} ", e.getMessage());
        }
        return null;
    }
    public long getTotalRecords(String indexName) throws Exception {
        CountRequest countRequest = CountRequest.of(c -> c.index(indexName));
        CountResponse countResponse = client.count(countRequest);
        return countResponse.count();
    }

}
