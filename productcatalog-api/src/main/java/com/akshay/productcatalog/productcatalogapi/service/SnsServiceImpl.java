package com.akshay.productcatalog.productcatalogapi.service;

import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.HashMap;
import java.util.Map;

@Service
public class SnsServiceImpl implements SnsService {

    private static final Logger logger = LoggerFactory.getLogger(SnsServiceImpl.class);

    @Value("${aws.sns.topic.arn:}")
    private String topicArn;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${app.domain.name:dev.akshaymchavan.me}")
    private String domainName;

    private SnsClient snsClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private MetricsService metricsService;

    public SnsServiceImpl() {
    }

    public SnsServiceImpl(SnsClient snsClient, String topicArn, String domainName) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
        this.domainName = domainName;
    }

    @PostConstruct
    public void init() {
        if (snsClient == null && topicArn != null && !topicArn.isBlank()) {
            try {
                this.snsClient = SnsClient.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
                logger.info("SNS Client successfully initialized for topic: {} in region: {}", topicArn, awsRegion);
            } catch (Exception e) {
                logger.error("Failed to initialize AWS SNS Client: {}", e.getMessage(), e);
            }
        } else if (topicArn == null || topicArn.isBlank()) {
            logger.info("No SNS topic ARN configured. SNS publishing will be bypassed (e.g. in test environment).");
        }
    }

    @Override
    public void publishUserSignup(User user, String token) {
        if (topicArn == null || topicArn.isBlank()) {
            logger.debug("Skipping SNS publish - SNS Topic ARN is not configured");
            return;
        }

        if (snsClient == null) {
            logger.warn("SNS Client is not initialized. Cannot publish message for user: {}", user.getUsername());
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> messagePayload = new HashMap<>();
            messagePayload.put("email", user.getUsername());
            messagePayload.put("firstName", user.getFirstName());
            messagePayload.put("lastName", user.getLastName());
            messagePayload.put("token", token);
            messagePayload.put("domain", domainName);
            messagePayload.put("timestamp", System.currentTimeMillis());

            String messageJson = objectMapper.writeValueAsString(messagePayload);

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messageJson)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("SNS message published successfully: messageId={}, user={}, duration={}ms",
                    response.messageId(), user.getUsername(), duration);

            if (metricsService != null) {
                metricsService.recordSnsPublishTime(duration);
                metricsService.incrementSnsPublishCount();
            }
        } catch (Exception e) {
            logger.error("Error publishing user creation message to SNS for {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }
}
