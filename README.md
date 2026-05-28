# EV Charging Station Booking Platform

Cloud-ready Spring Boot monolith for browsing EV charging stations on a map, inspecting connector availability, and reserving 30-minute charging slots. Roles: `DRIVER`, `ADMIN`.

## Stack

- Java 17, Spring Boot 3.3.x
- Spring MVC + Thymeleaf, Spring Security 6, Spring Data JPA
- PostgreSQL 16, Flyway migrations, Spring Session JDBC
- Leaflet + OpenStreetMap (map view)
- Bootstrap 5
- JUnit 5 + Testcontainers
- Heroku deployment (Java buildpack)

## Local development

### Prerequisites

- JDK 17 (Eclipse Temurin recommended)
- Maven 3.9+ *(only required to build outside Docker)*
- Docker Desktop *(for the local Postgres container)*

### Run Postgres

```bash
docker compose up -d
```

This starts Postgres 16 on `localhost:5432` with database/user/password `evbooking`.

### Run the app

```bash
mvn spring-boot:run
```

Or with the `dev` profile explicitly:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Open <http://localhost:8080>. The dev profile auto-seeds a bootstrap admin if `APP_ADMIN_BOOTSTRAP_EMAIL` and `APP_ADMIN_BOOTSTRAP_PASSWORD` are set; otherwise the first registered user must be promoted manually via SQL.

### Run tests

```bash
mvn test
```

Repository and concurrency tests use Testcontainers — Docker must be running.

## Deployment to Heroku

The project is preconfigured: `Procfile`, `system.properties` (Java 17), `app.json`.

### One-time setup

```bash
heroku create my-ev-booking
heroku addons:create heroku-postgresql:essential-0 --app my-ev-booking
heroku config:set SPRING_PROFILES_ACTIVE=prod                              --app my-ev-booking
heroku config:set APP_ADMIN_BOOTSTRAP_EMAIL=admin@example.com              --app my-ev-booking
heroku config:set APP_ADMIN_BOOTSTRAP_PASSWORD=<choose-a-strong-password>  --app my-ev-booking
```

Heroku Postgres exposes its connection details via the `DATABASE_URL` environment variable in the `postgres://user:pass@host:port/db` format. A small `@Configuration` class (`HerokuDataSourceConfig`) translates this into the `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` properties that Spring Boot expects.

### Deploy

```bash
git push heroku main
```

The Java buildpack runs `mvn -B -DskipTests package`, then `Procfile` launches the jar.

### Enable the `btree_gist` extension

The `EXCLUDE` constraint on `bookings` needs `btree_gist`. The Flyway migration creates it via `CREATE EXTENSION IF NOT EXISTS btree_gist`, which works on Heroku Postgres without extra steps.

### Logs

```bash
heroku logs --tail --app my-ev-booking
```

Each request is logged with: timestamp, HTTP method, URI, response status, processing time (ms), and instance id (the dyno name).

## Environment variables

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` |
| `DATABASE_URL` | Heroku-format Postgres URL (auto-injected by Heroku) |
| `SPRING_DATASOURCE_URL` | JDBC URL (auto-derived from `DATABASE_URL` in prod) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credentials (auto-derived in prod) |
| `APP_ADMIN_BOOTSTRAP_EMAIL` / `APP_ADMIN_BOOTSTRAP_PASSWORD` | First-run admin credentials |
| `APP_INSTANCE_ID` | Optional override for instance id in logs |
| `PORT` | Port to listen on (injected by Heroku) |

## Project layout

See [docs/architecture.md](docs/architecture.md) for the architecture and design rationale.

## License

Academic project — University of York coursework.
