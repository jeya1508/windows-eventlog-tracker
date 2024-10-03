package org.logging.service;

import org.logging.entity.AlertProfile;
import org.logging.exception.ValidationException;
import org.logging.repository.AlertProfileRepository;
import org.logging.to.AlertProfileTO;

import java.util.ArrayList;
import java.util.List;

public class AlertProfileService {
    private final AlertProfileRepository alertProfileRepository;
    private final ValidationService validationService;

    public AlertProfileService(AlertProfileRepository alertProfileRepository, ValidationService validationService) {
        this.alertProfileRepository = alertProfileRepository;
        this.validationService = validationService;
    }

    public String addProfile(AlertProfileTO alertProfileTO) throws Exception{
        AlertProfile alertProfile = alertProfileTO.toEntity();
        if(!alertProfileRepository.existsByProfileName(alertProfile.getProfileName()))
        {
            if(!validationService.isValidCriteria(alertProfile.getCriteria())) {
                throw new ValidationException("Invalid criteria format");
            }
            else if(!validationService.isValidEmail(alertProfile.getNotifyEmail())){
                throw new ValidationException("Invalid email format");
            }
            else{
                alertProfileRepository.save(alertProfile);
                return "Alert Profile added successfully";
            }
        }
        else{
            throw new Exception("Alert profile already exists");
        }
    }

    public String deleteProfile(String profileName) {
        if (alertProfileRepository.existsByProfileName(profileName)) {
            alertProfileRepository.deleteByProfileName(profileName);
            return "Alert Profile deleted successfully";
        } else {
            return "Alert Profile not found";
        }
    }

    public String updateProfile(AlertProfileTO alertProfileTO) throws Exception {
        AlertProfile alertProfile = alertProfileTO.toEntity();
        if (alertProfileRepository.existsByProfileName(alertProfile.getProfileName())) {
            if (!validationService.isValidCriteria(alertProfile.getCriteria())) {
                throw new ValidationException("Invalid criteria format");
            } else if (!validationService.isValidEmail(alertProfile.getNotifyEmail())) {
                throw new ValidationException("Invalid email format");
            } else {
                alertProfileRepository.updateProfile(alertProfile);
                return "Alert Profile updated successfully";
            }
        } else {
            throw new Exception("Alert profile not found");
        }
    }

    public List<AlertProfile> getAllProfiles() {
        return alertProfileRepository.findAll();
    }
}
