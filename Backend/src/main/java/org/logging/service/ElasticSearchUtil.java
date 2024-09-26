package org.logging.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.Map;

public class ElasticSearchUtil {
    Query parseKqlToQuery(String kqlQuery) {
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

     String getFieldType(String field) {
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
    public boolean logMatchesCriteria(Map<String, Object> logData, String criteria) {
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
