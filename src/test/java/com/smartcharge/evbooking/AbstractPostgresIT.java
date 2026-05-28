package com.smartcharge.evbooking;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers Postgres base for integration tests.
 * Uses a single container instance for the entire test JVM (set as static and reusable).
 */
public abstract class AbstractPostgresIT {

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("evbooking_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    static {
        POSTGRES.start();
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRES.getUsername(),
                "spring.datasource.password=" + POSTGRES.getPassword(),
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "app.seed.enabled=false"
            ).applyTo(ctx.getEnvironment());
        }
    }
}
