package com.smartcharge.evbooking.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Logs every HTTP request with the fields required by the assignment:
 *
 * <ul>
 *   <li>timestamp (ISO-8601)</li>
 *   <li>HTTP method</li>
 *   <li>requested URI (path + query)</li>
 *   <li>response status code</li>
 *   <li>processing time in milliseconds</li>
 *   <li>instance identifier (dyno name on Heroku, hostname otherwise)</li>
 * </ul>
 *
 * <p>Implemented as a servlet filter so it sits cross-cutting and outside the
 * controller layer; works identically for Thymeleaf pages and REST endpoints.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("access");

    private final String instanceId;

    public RequestLoggingFilter(@Value("${app.instance-id:unknown}") String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Instant start = Instant.now();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = Duration.between(start, Instant.now()).toMillis();
            String uri = request.getRequestURI()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

            log.info("ts={} method={} uri=\"{}\" status={} duration_ms={} instance={}",
                DateTimeFormatter.ISO_INSTANT.format(start),
                request.getMethod(),
                uri,
                response.getStatus(),
                ms,
                instanceId);
        }
    }
}
