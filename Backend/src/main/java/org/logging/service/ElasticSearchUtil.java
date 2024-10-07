package org.logging.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ElasticSearchUtil {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchUtil.class);
    public Query parseKqlToQuery(String kqlQuery) {
        String[] conditions = kqlQuery.split("\\s+");
        logger.info(conditions[0]);
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        for (String condition : conditions) {
            if (condition.contains("!=")) {
                String[] parts = condition.split("!=");
                String field = parts[0].trim();
                String value = parts[1].trim();
                String fieldType = getFieldType(field);

                switch (fieldType) {
                    case "keyword":
                        boolQueryBuilder.mustNot(mn -> mn.term(t -> t
                                .field(field + ".keyword")
                                .value(value)));
                        break;
                    case "integer":
                        boolQueryBuilder.mustNot(mn -> mn.term(t -> t
                                .field(field)
                                .value(Integer.parseInt(value))));
                        break;
                    case "date":
                        long epochMillis = Long.parseLong(value) * 1000;
                        boolQueryBuilder.mustNot(mn -> mn.term(t -> t
                                .field(field)
                                .value(epochMillis)));
                        break;
                    default:
                        logger.error("Unsupported field type for must_not condition: " + fieldType);
                        return null;
                }
            } else if (condition.contains("=")) {
                String[] parts = condition.split("=");
                String field = parts[0].trim();
                String value = parts[1].trim();
                String fieldType = getFieldType(field);
                logger.info("Field type is {}",fieldType);
                switch (fieldType) {
                    case "keyword":
                        boolQueryBuilder.must(m -> m.term(t -> t
                                .field(field + ".keyword")
                                .value(value)));
                        break;
                    case "integer":
                        boolQueryBuilder.must(m -> m.term(t -> t
                                .field(field)
                                .value(Integer.parseInt(value))));
                        break;
                    case "date":
                        long epochMillis = Long.parseLong(value) * 1000;
                        boolQueryBuilder.must(m -> m.term(t -> t
                                .field(field)
                                .value(epochMillis)));
                        break;
                    default:
                        logger.error("Unsupported field type for must condition: " + fieldType);
                        return null;
                }
            }
        }

        // Build and return the final query
        return Query.of(q -> q.bool(boolQueryBuilder.build()));
    }

    String getFieldType(String field) {
        if (field.equals("event_id") || field.equals("event_category")) {
            return "integer";
        } else if (field.equals("time_generated") || field.equals("time_written")) {
            return "date";
        } else {
            return "keyword";
        }
    }

    String getSortField(String sortBy) {
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
    public boolean logMatchesCriteria(Map<String, String> logData, String criteria) {
        String[] keyValue = criteria.split("=");
        if (keyValue.length == 2) {
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            if (logData.containsKey(key)) {
                String logValue = logData.get(key).toString().trim();
                return logValue.equalsIgnoreCase(value);
            }
        }
        return false;
    }
}
