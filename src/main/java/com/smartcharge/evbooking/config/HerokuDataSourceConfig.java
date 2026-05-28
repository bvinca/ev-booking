package com.smartcharge.evbooking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.boot.SpringApplication.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Heroku exposes the Postgres add-on connection details via the
 * {@code DATABASE_URL} environment variable in the form
 * {@code postgres://user:password@host:port/db}. Spring Boot expects the
 * standard {@code SPRING_DATASOURCE_URL} / {@code _USERNAME} / {@code _PASSWORD}
 * properties.
 *
 * <p>This {@link EnvironmentPostProcessor} runs before the application context
 * is built, parses {@code DATABASE_URL} if it is present, and contributes the
 * Spring-shaped properties as a high-precedence property source. It only acts
 * if {@code SPRING_DATASOURCE_URL} hasn't been explicitly set, so local
 * configuration (e.g. {@code application-dev.yml}) still wins.</p>
 *
 * <p>Registered via {@code META-INF/spring.factories}.</p>
 */
public class HerokuDataSourceConfig implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(HerokuDataSourceConfig.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (env.containsProperty("spring.datasource.url")) {
            return;
        }
        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(dbUrl.replace("postgres://", "postgresql://"));
            String userInfo = uri.getUserInfo();
            if (userInfo == null) {
                log.warn("DATABASE_URL missing credentials; not converting.");
                return;
            }
            String[] parts = userInfo.split(":", 2);
            String user = parts[0];
            String pass = parts.length > 1 ? parts[1] : "";
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + uri.getPath()
                + "?sslmode=require";

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrl);
            props.put("spring.datasource.username", user);
            props.put("spring.datasource.password", pass);
            env.getPropertySources().addFirst(new MapPropertySource("herokuDatabaseUrl", props));
            log.info("Configured Spring datasource from DATABASE_URL host={}", uri.getHost());
        } catch (Exception ex) {
            log.error("Failed to parse DATABASE_URL: {}", ex.getMessage());
        }
    }
}
