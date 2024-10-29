package org.logging.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.config.ElasticSearchConfig;
import org.logging.entity.DeviceInfo;
import org.logging.entity.LogInfo;
import org.logging.repository.ElasticSearchRepository;
import org.logging.service.DeviceService;
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
    private static DeviceService deviceService;
    private final ElasticSearchRepository elasticSearchRepository = new ElasticSearchRepository();
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServlet.class);

    @Override
    public void init() throws ServletException {
        ElasticsearchClient elasticsearchClient = ElasticSearchConfig.createElasticsearchClient();
        this.elasticSearchService = new ElasticSearchService(elasticsearchClient,validationService);
        objectMapper = new ObjectMapper();
        deviceService = new DeviceService();
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

    public void handleGetAllLogs(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long totalRecords = 0;
        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        String searchAfterParam = req.getParameter("searchAfter");
        String[] searchAfter = searchAfterParam!=null ? searchAfterParam.split(","): null;

        String deviceName = req.getParameter("deviceName") !=null ?req.getParameter("deviceName"):null;
        logger.info("Device name is {}", deviceName);
        DeviceInfo deviceInfo = (deviceName == null) ? null: deviceService.getDeviceFromDeviceName(deviceName) ;
        logger.info("Device info is {}",deviceInfo);

        String sortBy = req.getParameter("sortBy");
        String sortOrder = req.getParameter("sortOrder");

        List<LogInfo> logs = elasticSearchService.getAllLogs(deviceInfo, pageSize, searchAfter,sortBy,sortOrder);
        logger.info("Logs are {}",logs);

        String indexName = (deviceName == null) ? "windows-logs" : "windows-logs-"+deviceName;
        if(elasticSearchService.isIndexExists(indexName)) {
             totalRecords = elasticSearchRepository.getTotalRecords(indexName);
        }
        logger.info("Total records is {}",totalRecords);
        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("totalRecords", totalRecords);
        result.put("pageSize", pageSize);

        if (logs!=null && !logs.isEmpty()) {
            LogInfo lastLog = logs.getLast();
            result.put("searchAfter", lastLog.getSortValues());
        }

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.print(objectMapper.writeValueAsString(result));
        out.flush();
        out.close();
    }



    private void handleSearchLogs(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long totalRecords = 0;
        String query = req.getParameter("query");

        int pageSize = Integer.parseInt(req.getParameter("pageSize") != null ? req.getParameter("pageSize") : "10");
        String[] searchAfter = req.getParameterValues("searchAfter");

        String deviceName = req.getParameter("deviceName") !=null ?req.getParameter("deviceName"):null;
        DeviceInfo deviceInfo = (deviceName == null) ? null: deviceService.getDeviceFromDeviceName(deviceName) ;

        String indexName = (deviceName == null) ? "windows-logs" : "windows-logs-"+deviceName;

        List<LogInfo> logs = elasticSearchService.searchLogs(deviceInfo, query, pageSize,searchAfter);
        if(elasticSearchService.isIndexExists(indexName)) {
            totalRecords = elasticSearchService.getSearchedCount();
        }


        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("totalRecords", totalRecords);
        result.put("pageSize", pageSize);

        if (logs!=null && !logs.isEmpty()) {
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