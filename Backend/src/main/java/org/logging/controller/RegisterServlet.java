package org.logging.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logging.repository.UserRepository;
import org.logging.service.UserService;
import org.logging.service.ValidationService;
import org.logging.to.UserTO;
import org.logging.exception.UserAuthenticationException;
import org.logging.exception.ValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/v1/user/register")
public class RegisterServlet extends HttpServlet {

    private UserService userService;

    public RegisterServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        UserRepository userRepository = new UserRepository();
        ValidationService validationService = new ValidationService();

        userService = new UserService(userRepository, validationService);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws  IOException {
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
            String name = jsonNode.get("name").asText();
            String email = jsonNode.get("email").asText();
            String password = jsonNode.get("password").asText();

            UserTO userTO = new UserTO(name, email, password);
            String result = userService.addUser(userTO);

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", result);
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(objectMapper.writeValueAsString(successResponse));

        } catch (UserAuthenticationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(objectMapper.writeValueAsString(errorResponse));

        } catch (ValidationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(objectMapper.writeValueAsString(errorResponse));

        } catch (JsonProcessingException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid JSON processing");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(objectMapper.writeValueAsString(errorResponse));
        }

        out.flush();
        out.close();
    }
}