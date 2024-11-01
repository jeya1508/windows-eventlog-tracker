package org.logging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.logging.entity.User;
import org.logging.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.net.HttpURLConnection;

@WebServlet("/v1/user/google-callback")
public class GoogleCallbackServlet extends HttpServlet {

    private static final String CLIENT_ID = System.getenv("OAUTH_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("OAUTH_CLIENT_SECRET");
    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String REDIRECT_URI = "http://localhost:8500/servletlog/v1/user/google-callback";
    
    private UserRepository userRepository = new UserRepository();
    private static final Logger logger = LoggerFactory.getLogger(GoogleCallbackServlet.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = request.getParameter("code");
        if (code != null) {
            String tokenResponse = exchangeCodeForToken(code);

            ObjectNode tokenJson = new ObjectMapper().readValue(tokenResponse, ObjectNode.class);
            String accessToken = tokenJson.get("access_token").asText();

            String userInfo = fetchUserInfo(accessToken);

            ObjectNode userInfoJson = new ObjectMapper().readValue(userInfo, ObjectNode.class);
            String email = userInfoJson.get("email").asText();
            String name = userInfoJson.get("name").asText();

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                userRepository.save(newUser);
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("user", email);

            ObjectNode responseJson = new ObjectMapper().createObjectNode();
            responseJson.put("message", "Login successful");
            responseJson.put("email", email);
            responseJson.put("name", name);
            responseJson.put("sessionId", session.getId());

            response.setContentType("application/json");
            response.getWriter().write(responseJson.toString());
            response.setStatus(HttpServletResponse.SC_OK);

            response.sendRedirect("http://localhost:4200/login?sessionId=" + session.getId());
        } else {
            logger.error("Authorization code is missing");
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authorization code is missing");
            response.sendRedirect("http://localhost:4200/login");
        }
    }
    private String exchangeCodeForToken(String code) throws IOException {

        String tokenRequestBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&grant_type=" + URLEncoder.encode("authorization_code", StandardCharsets.UTF_8);

        HttpURLConnection tokenConnection = (HttpURLConnection) new URL(TOKEN_URI).openConnection();
        tokenConnection.setDoOutput(true);
        tokenConnection.setRequestMethod("POST");
        tokenConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = tokenConnection.getOutputStream()) {
            os.write(tokenRequestBody.getBytes());
        }

        int responseCode = tokenConnection.getResponseCode();

        if (responseCode == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(tokenConnection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            return response.toString();
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(tokenConnection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                errorResponse.append(line);
            }
            br.close();
            logger.error("Error in token exchange: {}", errorResponse);
            throw new IOException("Error in token exchange: " + errorResponse);
        }
    }

    private String fetchUserInfo(String accessToken) throws IOException {

        URL url = new URL(USER_INFO_URI + "?access_token=" + accessToken);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();

        return response.toString();
    }
}

