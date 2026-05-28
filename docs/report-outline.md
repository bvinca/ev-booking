# Coursework report outline (2000–3000 words)

This is the structural plan for the assignment's report deliverable. Word budgets are
indicative and total ~2700 words.

## 1. Introduction (~150 w)
- Problem statement: fragmented EV charging discovery / booking → double-bookings, poor
  visibility, manual admin overhead.
- Scope: server-side REST application, Thymeleaf + JS client with a map, RBAC, PaaS
  deployment, concurrent booking protection.
- Roadmap of the report.

## 2. System overview (~200 w)
- Architecture diagram (monolithic Spring Boot app, browser client with Leaflet,
  Postgres, Heroku).
- Stack: Java 17, Spring Boot 3.3, Spring MVC + Thymeleaf, Spring Security 6, Spring
  Data JPA, Flyway, Spring Session JDBC, PostgreSQL 16.
- Why these choices (mature, well-documented, free, exam-able).

## 3. Server-side design (~350 w)
- Layered monolith and why it suits this scope.
- Internal modularity: `domain` / `repository` / `service` / `web` / `security` /
  `config` / `infrastructure` packages, and what each contains.
- MVC separation, with one or two short code excerpts.
- IoC and constructor injection: include the `BookingService` constructor as an
  example; explain how the container wires beans and why `final` fields + constructor
  args matter for testability and immutability.
- Spring stereotype annotations table (`@Service`, `@Repository`, `@Controller`,
  `@RestController`).

## 4. Database design (~300 w)
- ER diagram (users, roles, user_roles, charging_stations, connectors, bookings).
- Normalisation: roles factored out; `connectors` references `stations` 1:N; `bookings`
  references both `users` and `connectors`.
- Indexes: `idx_bookings_connector_time` (partial index on CONFIRMED) speeds the
  overlap query; `idx_stations_city` speeds the city filter.
- Time columns: TIMESTAMPTZ, stored UTC.
- Schema lifecycle: Flyway as single source of truth; Hibernate validates only.

## 5. Client-side design (~250 w)
- Thymeleaf-first pages for SEO and simplicity; layout fragment reused across views.
- Dynamic interactivity via the REST API: Leaflet map fetches `/api/v1/stations`,
  availability grid fetches `/api/v1/connectors/{id}/availability`, booking POST uses
  the standard form-encoded round-trip.
- Bootstrap 5 for responsive design; no external CSS framework lock-in.
- Why Leaflet + OpenStreetMap rather than Google Maps: free, no API key, terms of use
  compatible with academic use.

## 6. REST API design (~200 w)
- Versioned prefix `/api/v1`.
- Resource-oriented URIs, HTTP verb semantics, status code conventions.
- Consistent JSON error shape produced by `RestExceptionHandler`.
- `Location` header on creation responses.
- Mention what is deliberately not REST: form-login flow stays MVC because Spring
  Security and CSRF handling fit that pattern best.

## 7. Authentication and RBAC (~250 w)
- Form login + BCrypt + `CustomUserDetailsService`.
- URL-level + method-level authorisation; defence in depth.
- Ownership checks at the service layer (`booking.isOwnedBy(userId)`), because the
  service has the domain knowledge.
- Roles: DRIVER, ADMIN. The brief says DRIVER, not USER.
- Bootstrap admin via env vars — no hard-coded passwords in source.

## 8. Concurrent booking protection (~400 w) — flagship section
- The race condition described concretely.
- **Layer 1**: pessimistic row lock on `connectors` via `SELECT ... FOR UPDATE`.
- **Layer 2**: overlap re-check inside the locked transaction.
- **Layer 3**: PostgreSQL `EXCLUDE USING gist` constraint on bookings as a
  database-level fail-safe. Include the DDL.
- Why pessimistic locking chosen over optimistic: contention is on a small, hot set of
  rows (popular connectors); retry storms under optimistic are wasteful.
- How this remains correct across multiple application instances: the lock and the
  constraint both live in Postgres.
- Reference to the concurrency test (`BookingServiceConcurrencyTest`).

## 9. Cloud deployment & externalised configuration (~300 w)
- PaaS chosen: Heroku, via the Java buildpack.
- Artefacts: `Procfile`, `system.properties`, `app.json`, `Dockerfile` (alternative path).
- `HerokuDataSourceConfig` translating the platform-injected `DATABASE_URL` into
  Spring's expected datasource properties — registered via
  `META-INF/spring.factories` so it runs before context refresh.
- Spring profiles (`dev`, `prod`) and which values come from env vars.
- Postgres add-on; `btree_gist` extension installed via Flyway.

## 10. Multi-instance execution (~200 w)
- Stateless processes: no in-memory caches of booking state.
- Sessions in DB via Spring Session JDBC; any dyno can serve any request.
- Booking safety in DB; correct under any number of dynos.
- Each log line carries the dyno's instance id so logs from `web.1` and `web.2` are
  distinguishable.

## 11. Logging & observability (~150 w)
- `RequestLoggingFilter` as a servlet filter at highest precedence; six required
  fields included.
- Why a filter (cross-cutting; same code for Thymeleaf and REST).
- Sample log line.
- `/actuator/health` for platform-level liveness checks.

## 12. Key implementation challenges and decisions (~200 w)
- **Concurrency strategy** — pessimistic + EXCLUDE constraint over optimistic.
- **Slot granularity** — fixed 30-minute slots simplify both UI and validation;
  considered free-form, rejected as harder to display and validate.
- **Thymeleaf + REST hybrid** — meets both "server-rendered with map" and "RESTful
  client" requirements without two front-ends.
- **Connector locking key** — locking `connectors` rather than `bookings` because the
  connector is the *resource being booked*, and any new booking must serialise against
  the row that represents it.

## 13. Conclusions (~150 w)
- What was delivered: a full, deployable monolith satisfying every brief requirement,
  with a concurrency-safety story that holds up under both unit tests and a 20-thread
  race test.
- What was intentionally deferred: payments, real-time websockets, microservices,
  Kubernetes.
- How the system would evolve at greater scale (catalogue / booking / identity split;
  Kubernetes; managed identity).
