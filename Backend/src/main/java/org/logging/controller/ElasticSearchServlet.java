package org.logging.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.config.ElasticSearchConfig;
import org.logging.entity.LogInfo;
import org.logging.repository.ElasticSearchRepository;
import org.logging.service.ElasticSearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.logging.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/v1/logs/*")
public class ElasticSearchServlet extends HttpServlet {

    private ElasticSearchService elasticSearchService;
    private ObjectMapper objectMapper;
    ValidationService validationService = new ValidationService();
    private final ElasticSearchRepository elasticSearchRepository = new ElasticSearchRepository();
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServlet.class);
    @Override
    public void init() throws ServletException {
        ElasticsearchClient elasticsearchClient = ElasticSearchConfig.createElasticsearchClient();
        this.elasticSearchService = new ElasticSearchService(elasticsearchClient,validationService);
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (!validationService.isAuthenticated(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            if ("/search/all".equals(pathInfo)) {
                handleGetAllLogs(req, resp);
            } else if ("/search".equals(pathInfo)) {
                handleSearchLogs(req, resp);
            }else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in retrieving logs from ES {}",e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleGetAllLogs(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        String searchAfterParam = req.getParameter("searchAfter");
        String[] searchAfter = searchAfterParam!=null ? searchAfterParam.split(","): null;

        String sortBy = req.getParameter("sortBy");
        String sortOrder = req.getParameter("sortOrder");

        List<LogInfo> logs = elasticSearchService.getAllLogs(pageSize, searchAfter,sortBy,sortOrder);
        long totalRecords = elasticSearchRepository.getTotalRecords("windows-event-logs");

        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("totalRecords", totalRecords);
        result.put("pageSize", pageSize);

        if (!logs.isEmpty()) {
            LogInfo lastLog = logs.getLast();
            result.put("searchAfter", lastLog.getSortValues());
        }

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.print(objectMapper.writeValueAsString(result));
        out.flush();
    }



    private void handleSearchLogs(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String query = req.getParameter("query");

        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        String[] searchAfter = req.getParameterValues("searchAfter");
        List<LogInfo> logs = elasticSearchService.searchLogs(query, pageSize,searchAfter);

        long totalRecords = elasticSearchService.getSearchedCount();

        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("totalRecords", totalRecords);
        result.put("pageSize", pageSize);

        if (!logs.isEmpty()) {
            LogInfo lastLog = logs.getLast();
            result.put("searchAfter", lastLog.getSortValues());
        }

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.print(objectMapper.writeValueAsString(result));
        out.flush();
    }

    @Override
    public void destroy()
    {
        ElasticSearchConfig.closeClient();
        logger.info("ES connection closed successfully");
    }
}