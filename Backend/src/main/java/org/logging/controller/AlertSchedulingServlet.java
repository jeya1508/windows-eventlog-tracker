package org.logging.controller;

import org.logging.config.ElasticSearchConfig;
import org.logging.repository.AlertProfileRepository;
import org.logging.service.AlertSchedulerService;
import org.logging.service.AlertScheduler;
import org.logging.service.ElasticSearchService;
import org.logging.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
@WebServlet(urlPatterns = {"/v1/alert/schedule"})
public class AlertSchedulingServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AlertSchedulingServlet.class);
    @Override
    public void init() throws ServletException {
        super.init();

        AlertProfileRepository alertProfileRepository = new AlertProfileRepository();
        ElasticSearchService elasticSearchService = new ElasticSearchService(ElasticSearchConfig.createElasticsearchClient());
        EmailService emailService = new EmailService();

        AlertSchedulerService alertSchedulerService = new AlertSchedulerService(alertProfileRepository, elasticSearchService, emailService);
        AlertScheduler alertScheduler = new AlertScheduler(alertSchedulerService);

        alertScheduler.start();
        logger.info("Alert Scheduler started");
    }
}
