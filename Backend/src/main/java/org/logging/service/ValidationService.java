package org.logging.service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.regex.Pattern;

public class ValidationService {

    public boolean isValidEmail(String email)
    {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return !email.isEmpty() && Pattern.compile(emailRegex).matcher(email).matches();
    }

    public boolean isValidPassword(String password)
    {
        String passwordRegex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";
        return !password.isEmpty() && Pattern.compile(passwordRegex).matcher(password).matches();
    }

    public boolean isValidCriteria(String criteria)
    {
        String criteriaRegex = "^[a-z_!]+=[a-zA-Z0-9-]+$";
        return !criteria.isEmpty() && Pattern.compile(criteriaRegex).matcher(criteria).matches();
    }
    public boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }
    public boolean isAuthenticated(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute("user") != null;
    }
    public boolean isValidIPAddress(String ipAddress)
    {
        String zeroTo255 = "(\\d{1,2}|(0|1)\\" + "d{2}|2[0-4]\\d|25[0-5])";
        String regex = zeroTo255 + "\\." + zeroTo255 + "\\."
                + zeroTo255 + "\\." + zeroTo255;
        return !ipAddress.isEmpty() && Pattern.compile(regex).matcher(ipAddress).matches();
    }
}

