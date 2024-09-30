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
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WebServlet("/v1/alert/*")
public class AlertRetrievalServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AlertRetrievalServlet.class);
    private AlertRetrievalService alertRetrievalService;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

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
            switch (pathInfo) {
                case "/all" : {
                    handleGetAllAlerts(req, resp);
                    break;
                }
                case "/search":{
                    handleSearchAlerts(req, resp);
                    break;
                }
                case "/allProfiles":{
                    handleGetAllProfiles(req, resp);
                    break;
                }
                case "/export/csv":{
                    handleExportAsCSV(req,resp);
                    break;
                }
                default:
                {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in processing request {}",e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleExportAsCSV(HttpServletRequest req, HttpServletResponse resp) {
        String kqlQuery = req.getParameter("query");
        String fileName = "exported_alerts.csv";

        resp.setContentType("text/csv");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (PrintWriter writer = resp.getWriter()) {
            List<AlertInfo> alerts = alertRetrievalService.getAlerts(kqlQuery);
            writer.write("Report Name: Alert Report\n");
            writer.write("Domain Name: All domain\n");
            writer.write("Number of records: "+alerts.size()+"\n");
            writer.write("Generated at: "+Instant.now()+"\n\n");


            writer.write("Profile Name,Source,Event type,Event id,Time generated\n");
            for (AlertInfo alert : alerts) {
                long epochSeconds = Long.parseLong(alert.getTime_generated());
                String timestamp = Instant.ofEpochSecond(epochSeconds)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter);

                writer.write(String.format("%s,%s,%s,%s,%s\n",
                        alert.getProfile_name(),
                        alert.getSource(),
                        alert.getEvent_type(),
                        alert.getEvent_id(),
                        timestamp
                ));
            }

            writer.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleGetAllProfiles(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        PrintWriter out = resp.getWriter();
        try{
            List<String> profileNames = alertRetrievalService.getAllProfiles();
            resp.setContentType("application/json");
            Map<String,Object> result = new HashMap<>();
            result.put("profileNames",profileNames);
            out.print(objectMapper.writeValueAsString(result));

            out.flush();
        }
        catch (Exception e)
        {
            logger.error("Error while retrieving profile names {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error in processing request");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(objectMapper.writeValueAsString(errorResponse));
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
    @Override
    public void destroy()
    {
        ElasticSearchConfig.closeClient();
        logger.info("ES connection closed successfully");
    }
}
