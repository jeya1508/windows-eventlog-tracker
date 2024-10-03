package org.logging.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.entity.ExportHistory;
import org.logging.service.S3Service;
import org.logging.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/v1/export/*")
public class AmazonS3Servlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AmazonS3Servlet.class);
    S3Service s3Service = new S3Service("event-log-bucket-1");
    private ObjectMapper objectMapper;
    ValidationService validationService = new ValidationService();
    @Override
    public void init() {
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
            if (pathInfo.equals("/all")) {
                handleGetAllExportFiles(req, resp);
            } 
            else if(pathInfo.equals("/history"))
            {
                handleExportHistory(req,resp);
            }
            else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        catch (Exception e)
        {
            logger.error("Error in processing GET request {}",e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("text/html");

        if (!validationService.isAuthenticated(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            switch (pathInfo) {
                case "/delete/all":
                {
                    s3Service.deleteAllCSVFilesFromS3();
                    resp.getWriter().write("All CSV files deleted ");
                    break;
                }
                case "/delete":
                {

                    StringBuilder json = new StringBuilder();
                    String line;
                    BufferedReader reader = req.getReader();
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                    }

                    JsonNode jsonNode = objectMapper.readTree(json.toString());
                    String fileName = jsonNode.get("fileName").asText();
                    s3Service.deleteCSVFileFromS3(fileName);
                    resp.getWriter().write("CSV file deleted from S3: " + fileName);
                    break;
                }
                default:
                {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in processing POST request {}",e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
    private void handleGetAllExportFiles(HttpServletRequest req, HttpServletResponse resp) {
        try{
            PrintWriter out = resp.getWriter();
            List<String> allCSVFilesFromS3 = s3Service.getAllCSVFilesFromS3();
            resp.setContentType("application/json");
            Map<String,Object> result = new HashMap<>();
            result.put("exportFiles",allCSVFilesFromS3);
            out.print(objectMapper.writeValueAsString(result));

            out.flush();
        }
        catch (Exception e)
        {
            logger.error("Error in retrieving csv files {}",e.getMessage());
        }
    }
    private void handleExportHistory(HttpServletRequest req, HttpServletResponse resp){
        List<String> csvFileNames = s3Service.getAllCSVFilesFromS3();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<ExportHistory> responseList = new ArrayList<>();

        for (String fileName : csvFileNames) {
            logger.info("File name is {}",fileName);
            String preSignedUrl = s3Service.generatePreSignedURL(fileName);
            responseList.add(new ExportHistory(fileName, preSignedUrl));
        }

        try (PrintWriter writer = resp.getWriter()) {
            String jsonResponse = objectMapper.writeValueAsString(responseList);
            writer.write(jsonResponse);
            writer.flush();
        }
        catch (IOException ioException)
        {
            logger.error("I/O Exception {}",ioException.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}
