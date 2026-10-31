package com.akshay.productcatalog.productcatalogapi.config;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatsDConfig {

    private static final Logger logger = LoggerFactory.getLogger(StatsDConfig.class);

    @Value("${management.statsd.metrics.export.host:localhost}")
    private String host;

    @Value("${management.statsd.metrics.export.port:8125}")
    private int port;

    @Value("${management.statsd.metrics.export.prefix:csye6225}")
    private String prefix;

    @Value("${management.statsd.metrics.export.enabled:true}")
    private boolean enabled;

    @Bean
    public StatsDClient statsDClient() {
        if (!enabled) {
            logger.info("StatsD metrics client disabled via configuration. Using NoOpStatsDClient.");
            return new NoOpStatsDClient();
        }

        try {
            logger.info("Initializing NonBlockingStatsDClient connecting to {}:{} with prefix '{}'", host, port, prefix);
            return new NonBlockingStatsDClient(prefix, host, port);
        } catch (Exception e) {
            logger.error("Failed to initialize StatsD client. Falling back to NoOpStatsDClient.", e);
            return new NoOpStatsDClient();
        }
    }
}
