package org.logging.service;

import org.logging.entity.User;
import org.logging.exception.UserAuthenticationException;
import org.logging.exception.ValidationException;
import org.logging.repository.UserRepository;
import org.logging.to.AuthResponseTO;
import org.logging.to.SignInTO;
import org.logging.to.UserTO;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;


public class UserService {
    private final UserRepository userRepository;
    private final ValidationService validationService;

    public UserService(UserRepository userRepository,
                       ValidationService validationService) {
        this.userRepository = userRepository;
        this.validationService = validationService;
    }

    public String addUser(UserTO userTO) throws UserAuthenticationException, ValidationException {
        User user = userTO.toEntity();

        if (!userRepository.existsByEmail(user.getEmail())) {
            if (validationService.isValidEmail(user.getEmail()) && validationService.isValidPassword(user.getPassword())) {
                String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
                user.setPassword(hashedPassword);

                userRepository.save(user);
                return "User added successfully";
            } else if (!validationService.isValidEmail(user.getEmail())) {
                throw new ValidationException("Invalid Email");
            } else if (!validationService.isValidPassword(user.getPassword())) {
                throw new ValidationException("Invalid Password");
            } else {
                throw new ValidationException("Error in user registration");
            }
        } else {
            throw new UserAuthenticationException("Error in user registration");
        }
    }
    public AuthResponseTO login(SignInTO signInTO, HttpServletRequest request) throws UserAuthenticationException {
        Optional<User> userOpt = userRepository.findByEmail(signInTO.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (BCrypt.checkpw(signInTO.getPassword(), user.getPassword())) {
                HttpSession session = request.getSession(true);
                String sessionId = session.getId();
                String message = "User logged in successfully";
                return new AuthResponseTO(sessionId, message);
            } else {
                throw new UserAuthenticationException("Invalid password");
            }
        } else {
            throw new UserAuthenticationException("User not found");
        }
    }

}

