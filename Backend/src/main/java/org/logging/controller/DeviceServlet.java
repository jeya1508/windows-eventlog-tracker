package org.logging.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.config.ElasticSearchConfig;
import org.logging.entity.DeviceInfo;
import org.logging.entity.LogInfo;
import org.logging.exception.ValidationException;
import org.logging.repository.ElasticSearchRepository;
import org.logging.service.DeviceService;
import org.logging.service.ElasticSearchService;
import org.logging.service.LoggingService;
import org.logging.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/v1/device/*")
public class DeviceServlet extends HttpServlet
{
    private ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(DeviceServlet.class);
    private static ValidationService validationService;
    private static DeviceService deviceService;
    private static ElasticSearchServlet elasticSearchServlet;
    private final ElasticSearchRepository elasticSearchRepository = new ElasticSearchRepository();

    @Override
    public void init()
    {
        objectMapper = new ObjectMapper();
        validationService = new ValidationService();
        deviceService = new DeviceService();
        elasticSearchServlet = new ElasticSearchServlet();

    }
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    {
        String pathInfo = request.getPathInfo();
        logger.info("The path is {}",pathInfo);
        if (!validationService.isAuthenticated(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            if (pathInfo.equals("/all")) {
                getAllDevices(request, response);
            }
            else if (pathInfo.equals("/find"))
            {
                getLogsFromDevices(request,response);
            }else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        catch (Exception e) {
            logger.error("Error in processing Device servlet GET request {}",e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    private void getLogsFromDevices(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String deviceName = request.getParameter("deviceName");
        DeviceInfo deviceInfo = deviceService.getDeviceFromDeviceName(deviceName);

        LoggingService.collectWindowsLogs(deviceInfo.getIpAddress(),deviceInfo.getHostName(),deviceInfo.getPassword());
        elasticSearchServlet.handleGetAllLogs(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (!validationService.isAuthenticated(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if(pathInfo.equals("/add"))
        {
            addDevice(request,response);
        }
        else{
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void addDevice(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        try{

            response.setContentType("application/json");
            JsonNode jsonNode = objectMapper.readTree(request.getReader());

            String deviceName = jsonNode.get("deviceName").asText();
            String ipAddress = jsonNode.get("ipAddress").asText();
            String hostName = jsonNode.get("hostName").asText();
            String password = jsonNode.get("password").asText();

            String result = deviceService.addDeviceToFile(deviceName,ipAddress, hostName, password);

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", result);
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(objectMapper.writeValueAsString(successResponse));
        }
        catch (ValidationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(objectMapper.writeValueAsString(errorResponse));

        }
        catch (IOException e)
        {
            logger.error("Error in getting the JSON object from the request {}",e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error",e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    public void getAllDevices(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<String> deviceList = deviceService.getAllDeviceName();
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), deviceList);
    }
}
