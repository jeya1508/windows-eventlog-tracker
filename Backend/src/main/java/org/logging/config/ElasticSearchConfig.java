package org.logging.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

public class ElasticSearchConfig {
    private static final ElasticsearchClient client = createElasticsearchClient();

    private static RestClient restClient; // Get RestClient from ElasticsearchClient

    public static ElasticsearchClient createElasticsearchClient() {
        String username = "elastic";
        String password = "elastic";

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setDefaultRequestConfig(RequestConfig.custom()
                                .setConnectTimeout(120000)
                                .setSocketTimeout(120000)
                                .build()));

        restClient = builder.build();

        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }
    public static void closeClient() {
        try {
            // Close the RestClient to release resources
            restClient.close();
            System.out.println("Elasticsearch client closed successfully.");
        } catch (Exception e) {
            System.err.println("Failed to close Elasticsearch client: " + e.getMessage());
        }
    }
}
