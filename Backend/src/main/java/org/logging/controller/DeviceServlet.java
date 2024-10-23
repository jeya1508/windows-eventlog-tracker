package org.logging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/v1/device/*")
public class DeviceServlet extends HttpServlet {
    public static final String absolutePath = "C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\Backend\\src\\main\\java\\org\\logging\\assets\\deviceFile.txt";
    private ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(DeviceServlet.class);
    private static ValidationService validationService;
    @Override
    public void init()
    {
        objectMapper = new ObjectMapper();
        validationService = new ValidationService();
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
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        catch (Exception e) {
            logger.error("Error in processing Device servlet GET request {}",e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    public void getAllDevices(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<String> deviceList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(absolutePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                deviceList.add(line.trim());
            }
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading devices file: " + e.getMessage());
            return;
        }
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), deviceList);
    }
}
