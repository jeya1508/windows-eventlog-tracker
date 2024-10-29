package org.logging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.logging.exception.UserAuthenticationException;
import org.logging.repository.UserRepository;
import org.logging.service.UserService;
import org.logging.service.ValidationService;
import org.logging.to.AuthResponseTO;
import org.logging.to.SignInTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/v1/user/login")
public class LoginServlet extends HttpServlet {

    private UserService userService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);
    private static final String CLIENT_ID = System.getenv("OAUTH_CLIENT_ID");
    private static final String REDIRECT_URI = "http://localhost:8500/servletlog/v1/user/google-callback";
    private static final String AUTH_URI = "https://accounts.google.com/o/oauth2/auth";
    @Override
    public void init() throws ServletException {
        super.init();
         userRepository = new UserRepository();
        ValidationService validationService = new ValidationService();

        userService = new UserService(userRepository, validationService);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            BufferedReader reader = request.getReader();
            SignInTO signInTO = objectMapper.readValue(reader, SignInTO.class);

            AuthResponseTO authResponse = userService.login(signInTO, request);

            HttpSession session = request.getSession(true);
            session.setAttribute("user", signInTO.getEmail());

            ObjectNode responseJson = objectMapper.createObjectNode();
            responseJson.put("sessionId", authResponse.getSessionId());
            responseJson.put("message", authResponse.getMessage());

            response.setStatus(HttpServletResponse.SC_OK);
            out.write(responseJson.toString());

        } catch (UserAuthenticationException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ObjectNode errorJson = objectMapper.createObjectNode();
            errorJson.put("error", e.getMessage());
            out.write(errorJson.toString());

        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ObjectNode errorJson = objectMapper.createObjectNode();
            errorJson.put("error", "Invalid JSON payload");
            out.write(errorJson.toString());
        }

        out.flush();
        out.close();
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException
    {
        try {
            String oauthUrl = AUTH_URI + "?client_id=" + CLIENT_ID
                    + "&redirect_uri=" + REDIRECT_URI
                    + "&response_type=code"
                    + "&scope=profile email";

            resp.sendRedirect(oauthUrl);
        }
        catch(IOException e)
        {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.error("Exception while redirecting to the URL {}",e.getMessage());
        }
    }
    @Override
    public void destroy()
    {
        userRepository.closeConnection();
        logger.info("Mongo connection closed successfully");
    }
}