package com.smartcharge.evbooking;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Shared base for integration tests that need a real Postgres database.
 *
 * <p>Points at a local Postgres database called {@code evbooking_test} so the
 * tests can run without Docker. Override any of the connection details via
 * system properties or environment variables:</p>
 *
 * <ul>
 *   <li>{@code TEST_DB_URL}      (default {@code jdbc:postgresql://localhost:5432/evbooking_test})</li>
 *   <li>{@code TEST_DB_USERNAME} (default {@code postgres})</li>
 *   <li>{@code TEST_DB_PASSWORD} (default {@code admin})</li>
 * </ul>
 *
 * <p><b>One-time setup:</b> in pgAdmin, create a database called
 * {@code evbooking_test} (any owner), then on that DB run
 * {@code CREATE EXTENSION IF NOT EXISTS btree_gist;}.</p>
 *
 * <p>The Flyway migrations are applied automatically by Spring Boot on test
 * startup, and {@code spring.flyway.clean-disabled=false} lets us reset the
 * schema between test classes that need a clean slate.</p>
 */
public abstract class AbstractPostgresIT {

    protected static final String JDBC_URL = pickProperty(
        "TEST_DB_URL", "jdbc:postgresql://localhost:5432/evbooking_test");
    protected static final String DB_USER  = pickProperty("TEST_DB_USERNAME", "postgres");
    protected static final String DB_PASS  = pickProperty("TEST_DB_PASSWORD", "admin");

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            TestPropertyValues.of(
                "spring.datasource.url=" + JDBC_URL,
                "spring.datasource.username=" + DB_USER,
                "spring.datasource.password=" + DB_PASS,
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.clean-disabled=false",
                "spring.session.jdbc.initialize-schema=always",
                "app.seed.enabled=false",
                "app.admin-bootstrap.email=",
                "app.admin-bootstrap.password="
            ).applyTo(ctx.getEnvironment());
        }
    }

    private static String pickProperty(String key, String fallback) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
