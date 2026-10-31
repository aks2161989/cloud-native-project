package com.akshay.productcatalog.productcatalogapi.config;

import com.akshay.productcatalog.productcatalogapi.service.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestMetricsFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestMetricsFilter.class);

    private final MetricsService metricsService;

    public RequestMetricsFilter(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String remoteAddr = request.getRemoteAddr();

        logger.info("Incoming HTTP request: method={}, path={}, remoteAddr={}", method, uri, remoteAddr);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            metricsService.incrementApiCall(method, uri);
            metricsService.recordApiCallTime(method, uri, duration);

            logger.info("Completed HTTP request: method={}, path={}, status={}, duration={}ms",
                    method, uri, status, duration);
        }
    }
}
