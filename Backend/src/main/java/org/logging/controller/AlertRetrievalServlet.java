package org.logging.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.config.ElasticSearchConfig;
import org.logging.entity.AlertInfo;
import org.logging.exception.ValidationException;
import org.logging.repository.ElasticSearchRepository;
import org.logging.service.AlertRetrievalService;
import org.logging.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/v1/alert/*")
public class AlertRetrievalServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AlertRetrievalServlet.class);
    private AlertRetrievalService alertRetrievalService;
    private ObjectMapper objectMapper;
    ValidationService validationService = new ValidationService();
    ElasticSearchRepository elasticSearchRepository = new ElasticSearchRepository();

    @Override
    public void init() {
        ElasticsearchClient elasticsearchClient = ElasticSearchConfig.createElasticsearchClient();
        this.alertRetrievalService = new AlertRetrievalService(elasticsearchClient,validationService);
        objectMapper = new ObjectMapper();
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (!validationService.isAuthenticated(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            if ("/all".equals(pathInfo)) {
                handleGetAllAlerts(req, resp);
            } else if ("/search".equals(pathInfo)) {
                handleSearchAlerts(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in processing request {}",e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    public void handleGetAllAlerts(HttpServletRequest req, HttpServletResponse resp) throws Exception
    {
        PrintWriter out = resp.getWriter();

        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        String searchAfterParam = req.getParameter("searchAfter");
        String[] searchAfter = searchAfterParam != null ? searchAfterParam.split(",") : null;
        try {
            List<AlertInfo> logs = alertRetrievalService.getAllAlerts(pageSize, searchAfter);
            long totalRecords = elasticSearchRepository.getTotalRecords("alerts");
            Map<String, Object> result = new HashMap<>();
            result.put("logs", logs);
            result.put("totalRecords", totalRecords);
            result.put("pageSize", pageSize);

            if (!logs.isEmpty()) {
                AlertInfo lastLog = logs.get(logs.size() - 1);
                result.put("searchAfter", lastLog.getSortValues());
            }

            resp.setContentType("application/json");
            out.print(objectMapper.writeValueAsString(result));

        }
        catch (Exception e) {
            logger.error("Error while retrieving alerts {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error in processing request");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(objectMapper.writeValueAsString(errorResponse));
        }
        out.flush();
    }
    public void handleSearchAlerts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        PrintWriter out = resp.getWriter();

        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        String[] searchAfter = req.getParameterValues("searchAfter");

        try {
            List<AlertInfo> logs = alertRetrievalService.searchAlerts(query, pageSize, searchAfter);

            long totalRecords = alertRetrievalService.getSearchedCount();

            Map<String, Object> result = new HashMap<>();
            result.put("logs", logs);
            result.put("totalRecords", totalRecords);
            result.put("pageSize", pageSize);

            if (!logs.isEmpty()) {
                AlertInfo lastLog = logs.getLast();
                result.put("searchAfter", lastLog.getSortValues());
            }
            resp.setContentType("application/json");
            out.print(objectMapper.writeValueAsString(result));
        }
        catch (ValidationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(objectMapper.writeValueAsString(errorResponse));

        }
        catch(Exception e)
        {
            logger.error("Error while processing search query {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error in processing request");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(objectMapper.writeValueAsString(errorResponse));
        }

        out.flush();
    }
}
