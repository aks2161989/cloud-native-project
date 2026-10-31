package com.akshay.productcatalog.productcatalogapi.config;

import com.akshay.productcatalog.productcatalogapi.service.MetricsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DatabaseMetricsAspect {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetricsAspect.class);

    private final MetricsService metricsService;

    public DatabaseMetricsAspect(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Around("execution(* com.akshay.productcatalog.productcatalogapi.repository..*(..))")
    public Object timeDatabaseQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String repositoryName = joinPoint.getTarget().getClass().getInterfaces().length > 0
                ? joinPoint.getTarget().getClass().getInterfaces()[0].getSimpleName()
                : joinPoint.getTarget().getClass().getSimpleName();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordDbQueryTime(methodName, duration);
            logger.debug("Database query executed: repository={}, method={}, duration={}ms",
                    repositoryName, methodName, duration);
        }
    }
}
