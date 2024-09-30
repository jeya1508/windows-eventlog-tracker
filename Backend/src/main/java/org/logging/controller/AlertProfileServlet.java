package org.logging.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.exception.ValidationException;
import org.logging.repository.AlertProfileRepository;
import org.logging.service.AlertProfileService;
import org.logging.service.ValidationService;
import org.logging.to.AlertProfileTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/v1/alert/add")
public class AlertProfileServlet extends HttpServlet {
    private AlertProfileService alertService;
    private AlertProfileRepository alertProfileRepository;
    private static final Logger logger = LoggerFactory.getLogger(AlertProfileServlet.class);

    public AlertProfileServlet()
    {
        super();
    }
    @Override
    public void init() throws ServletException {
        super.init();
        alertProfileRepository = new AlertProfileRepository();
        ValidationService validationService = new ValidationService();
        alertService = new AlertProfileService(alertProfileRepository,validationService);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            StringBuilder json = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            JsonNode jsonNode = objectMapper.readTree(json.toString());
            String profileName = jsonNode.get("profileName").asText();
            String criteria = jsonNode.get("criteria").asText();
            String notifyEmail = jsonNode.get("notifyEmail").asText();

            AlertProfileTO alertProfileTO = new AlertProfileTO(profileName,criteria,notifyEmail);
            String result= alertService.addProfile(alertProfileTO);
            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", result);
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(objectMapper.writeValueAsString(successResponse));
        }
        catch (JsonProcessingException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid JSON processing");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(objectMapper.writeValueAsString(errorResponse));
        }
        catch (ValidationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(objectMapper.writeValueAsString(errorResponse));

        }
        catch (Exception e)
        {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Alert profile adding failed");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(objectMapper.writeValueAsString(errorResponse));
        }
        out.flush();
        out.close();
    }
    @Override
    public void destroy()
    {
        alertProfileRepository.closeConnection();
        logger.info("MongoDB connection closed successfully");
    }
}
