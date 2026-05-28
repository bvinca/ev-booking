# Heroku deployment guide

This project deploys to Heroku via the **Java buildpack**. The supporting artefacts already live in the repo root:

| File | Purpose |
|---|---|
| `Procfile` | Tells Heroku how to launch the web dyno (`java -jar target/ev-booking.jar`). |
| `system.properties` | Pins the JVM to Java 17 on the Heroku platform. |
| `app.json` | "Deploy to Heroku" / Review-App manifest declaring the Postgres add-on and required env vars. |
| `src/main/resources/META-INF/spring.factories` | Registers `HerokuDataSourceConfig`, which translates Heroku's `DATABASE_URL` into Spring datasource properties. |

## One-time setup

```bash
# Sign in and create the app (region is optional; default is US)
heroku login
heroku create my-ev-booking --region eu

# Attach a Postgres add-on. The free Essential-0 plan suits coursework.
heroku addons:create heroku-postgresql:essential-0 --app my-ev-booking

# Configure the runtime
heroku config:set SPRING_PROFILES_ACTIVE=prod                                    --app my-ev-booking
heroku config:set APP_ADMIN_BOOTSTRAP_EMAIL=admin@example.com                    --app my-ev-booking
heroku config:set APP_ADMIN_BOOTSTRAP_PASSWORD='<strong-password>'               --app my-ev-booking
heroku config:set JAVA_OPTS='-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0' --app my-ev-booking
```

> `DATABASE_URL` is set automatically when the add-on is provisioned. `PORT` is injected on every dyno boot.

## Deploy

```bash
git push heroku main
```

The Java buildpack does:

1. Detects `pom.xml`
2. Runs `mvn -B -DskipTests clean install`
3. Boots the dyno with the command in `Procfile`

On startup:

1. `HerokuDataSourceConfig` reads `DATABASE_URL`, splits it into `spring.datasource.url/username/password`, and contributes them to the Spring `Environment`.
2. Flyway runs `V1__init_schema.sql`, creates the `btree_gist` extension, all tables, indexes, and the `no_overlapping_confirmed_bookings` EXCLUDE constraint.
3. Spring Session JDBC creates the `SPRING_SESSION` table.
4. `AdminBootstrap` creates the first admin if none exists.
5. The app starts on `$PORT`.

## Verifying multi-instance support

Heroku dynos are stateless and may be scaled horizontally:

```bash
heroku ps:scale web=2 --app my-ev-booking
```

The app remains correct under multi-instance execution because:

- HTTP sessions are persisted in Postgres via Spring Session JDBC, so any dyno can serve any request after login.
- Booking concurrency safety lives in Postgres (pessimistic lock + EXCLUDE constraint) and therefore holds across dynos.
- Each log line carries `instance=<DYNO_NAME>` (e.g. `web.1`, `web.2`) so multi-instance log streams stay distinguishable.

## Verifying request logging

```bash
heroku logs --tail --app my-ev-booking
```

Sample line:

```
2026-06-01T09:14:53.812Z INFO  [web.1] [http-nio-...] access -
  ts=2026-06-01T09:14:53.798Z method=GET uri="/stations" status=200 duration_ms=14 instance=web.1
```

The fields satisfy the assignment's request-logging requirements: timestamp, method, URI, status, duration, and instance id.

## Quick smoke test

```bash
heroku open --app my-ev-booking
# 1. Visit /stations             → list + map render
# 2. /register, then /login      → form-login works
# 3. /admin (logged-in driver)   → 403 page
# 4. Login as bootstrap admin    → /admin loads
```

## Common issues

- **`Driver org.postgresql.Driver claims to not accept jdbcUrl, postgres://...`** — `HerokuDataSourceConfig` didn't run. Verify `META-INF/spring.factories` is on the classpath inside the built jar.
- **`relation "spring_session" does not exist`** — Spring Session JDBC schema init is disabled. Set `spring.session.jdbc.initialize-schema=always` (already done in `application.yml`).
- **`extension "btree_gist" is not available`** — Heroku Postgres supports it; the Flyway migration installs it. If it fails, run `heroku pg:psql -c 'CREATE EXTENSION btree_gist;'`.

## Rollback

Heroku keeps every release:

```bash
heroku releases --app my-ev-booking
heroku rollback v<N> --app my-ev-booking
```
