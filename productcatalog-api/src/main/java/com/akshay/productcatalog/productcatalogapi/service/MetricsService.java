package com.akshay.productcatalog.productcatalogapi.service;

import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final StatsDClient statsDClient;

    public MetricsService(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    public void increment(String metric) {
        try {
            statsDClient.increment(metric);
        } catch (Exception e) {
            logger.warn("Failed to increment metric: {}", metric, e);
        }
    }

    public void recordExecutionTime(String metric, long durationMs) {
        try {
            statsDClient.recordExecutionTime(metric, durationMs);
        } catch (Exception e) {
            logger.warn("Failed to record execution time for metric: {}", metric, e);
        }
    }

    public void incrementApiCall(String method, String endpoint) {
        String sanitized = sanitizeEndpoint(endpoint);
        String methodLower = method.toLowerCase();

        // Specific endpoint counter: e.g. endpoint.v1.user.post.count
        increment("endpoint." + sanitized + "." + methodLower + ".count");
        // General API counter:
        increment("api.request.count");
    }

    public void recordApiCallTime(String method, String endpoint, long durationMs) {
        String sanitized = sanitizeEndpoint(endpoint);
        String methodLower = method.toLowerCase();

        // Specific endpoint timer: e.g. endpoint.v1.user.post.time
        recordExecutionTime("endpoint." + sanitized + "." + methodLower + ".time", durationMs);
        // General API timer:
        recordExecutionTime("api.request.time", durationMs);
    }

    public void recordDbQueryTime(long durationMs) {
        recordExecutionTime("db.query.time", durationMs);
    }

    public void recordDbQueryTime(String operation, long durationMs) {
        recordExecutionTime("db.query.time", durationMs);
        if (operation != null && !operation.isBlank()) {
            recordExecutionTime("db." + operation.toLowerCase() + ".time", durationMs);
        }
    }

    public void recordS3CallTime(long durationMs) {
        recordExecutionTime("s3.call.time", durationMs);
    }

    public void recordS3CallTime(String operation, long durationMs) {
        recordExecutionTime("s3.call.time", durationMs);
        if (operation != null && !operation.isBlank()) {
            recordExecutionTime("s3." + operation.toLowerCase() + ".time", durationMs);
        }
    }

    public void incrementSnsPublishCount() {
        increment("sns.publish.count");
    }

    public void recordSnsPublishTime(long durationMs) {
        recordExecutionTime("sns.publish.time", durationMs);
    }

    private String sanitizeEndpoint(String uri) {
        if (uri == null || uri.isBlank() || uri.equals("/")) {
            return "root";
        }
        String cleaned = uri.replaceAll("^/+", "").replaceAll("/+$", "");
        // Replace UUIDs with placeholder 'id'
        cleaned = cleaned.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "id");
        // Replace numeric path segments with 'id'
        cleaned = cleaned.replaceAll("/\\d+", "/id");
        cleaned = cleaned.replace('/', '.');
        return cleaned.isEmpty() ? "root" : cleaned;
    }
}
