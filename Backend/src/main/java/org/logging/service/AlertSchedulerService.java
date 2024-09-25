package org.logging.service;

import org.logging.entity.AlertProfile;
import org.logging.entity.LogInfo;
import org.logging.repository.AlertProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AlertSchedulerService {

    private final AlertProfileRepository alertProfileRepository;
    private final ElasticSearchService elasticSearchService;
    private final EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(AlertSchedulerService.class);

    public AlertSchedulerService(AlertProfileRepository alertProfileRepository,
                                 ElasticSearchService elasticSearchService,
                                 EmailService emailService) {
        this.alertProfileRepository = alertProfileRepository;
        this.elasticSearchService = elasticSearchService;
        this.emailService = emailService;
    }

    public void checkAndTriggerAlerts() throws Exception {
        List<AlertProfile> profiles = alertProfileRepository.findAll();

        for (AlertProfile profile : profiles) {
            List<LogInfo> matchingLogs = elasticSearchService.searchLogs(profile.getCriteria(),5,null);
            long totalRecords = elasticSearchService.getSearchedCount();
            if (!matchingLogs.isEmpty()) {
                String alertMessage = createAlertMessage(profile.getProfileName(),totalRecords);
                triggerAlert(profile, profile.getNotifyEmail(),alertMessage);
            }
        }
    }
    private String createAlertMessage(String profileName,long total) {

        String message = "New alerts generated for profile: " + profileName + "\n\n" + "Total number of logs matching the criteria: " + total + "\n" +
                "To view all alerts, go to the mentioned url: http://localhost:4200/" + "\n";

        return message;
    }
    private void triggerAlert(AlertProfile profile,String recipientMail, String emailBody) {
        try {
            emailService.sendEmail(recipientMail, "Check your Windows log alerts", emailBody);
            logger.info("Alert email sent successfully for profile: {}", profile.getProfileName());
        } catch (Exception e) {
            logger.error("Failed to send alert email for profile: {} " , profile.getProfileName());
            logger.error(e.getMessage());
        }
    }
}
