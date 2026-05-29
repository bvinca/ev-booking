# EV Charging Station Booking Platform — Implementation Report

**Author:** Bora Vinca
**Module:** University of York coursework
**Date:** May 2026
**Live deployment:** <https://ev-booking-a45c014d831e.herokuapp.com/>
**Source repository:** <https://github.com/bvinca/ev-booking>

---

## 1. Introduction

This report documents the design and implementation of a cloud-ready booking
platform for electric-vehicle charging stations. The system replaces the
fragmented, manually administered process described in the brief with a single
web application in which drivers can browse stations on a map, inspect
connector availability, and reserve 30-minute charging slots in a way that is
both ergonomic and free of double-booking. Two roles are supported: `DRIVER`,
who manages their own bookings, and `ADMIN`, who manages stations, connectors,
and any booking in the database.

The artefact is a Spring Boot 3.3 monolith, deployed to Heroku and backed by a
Postgres database. The code is organised into clearly separated layers
(controller, service, repository, domain) and exposes both server-rendered
Thymeleaf pages and a JSON REST API. A pessimistic database lock combined with
a PostgreSQL `EXCLUDE` constraint guarantees that concurrent attempts to book
the same slot cannot succeed simultaneously. A request-logging filter captures
the operational metadata the assignment requires, and the application is
written so that multiple instances can run safely behind a single hostname.

The remainder of the report walks through each design area in turn — server
side, database, client side, authentication and RBAC, concurrent booking
protection, cloud deployment, externalised configuration, multi-instance
execution, and a short reflection on the implementation challenges. The
codebase is organised so that any specific claim made here can be located in
the source within seconds.

## 2. Server-side design

The server is a single Spring Boot application laid out as a layered monolith.
Each package corresponds to one architectural responsibility: `domain` holds
JPA entities; `repository` holds Spring Data interfaces; `service` holds
business logic and transactions; `web.controller` and `web.rest` hold the two
flavours of controller; `web.dto` and `web.mapper` translate between the wire
format and the domain; `security` adapts JPA users to Spring Security; and
`infrastructure` collects cross-cutting concerns such as the request-logging
filter and the development seeder.

The layering is enforced by convention rather than by build-time tooling, but
the conventions are strict: controllers depend only on services, services
depend only on repositories and other services, and entities are mapped to
DTOs before they cross the controller boundary. Cross-cutting concerns live as
filters or `@ControllerAdvice` classes outside the layered chain, so the
controller code itself stays small and obvious.

A monolithic architecture was chosen deliberately. The domain is tightly
coupled — a booking is meaningless without a connector, a connector without a
station — and the most important operation in the system (creating a booking)
is a single, strongly consistent transaction. Splitting that into
microservices would require either distributed transactions or a saga pattern,
both of which add complexity and failure modes with no benefit at the scale of
this coursework. The package-level modularity above leaves an obvious
extraction path if the system grew to need it (a `bookings` service, a
`catalogue` service, an `auth` service), but pursuing that split now would be
premature.

Inversion of control and dependency injection are used throughout. Every
collaborator is supplied via constructor injection, every dependency is
declared `private final`, and there are no `@Autowired` field injections. This
keeps the dependency graph explicit at the constructor signature, makes every
class trivially unit-testable with mocks, and prevents the kind of accidental
state-sharing that field injection encourages. The four standard Spring
stereotypes — `@Controller`, `@RestController`, `@Service`, `@Repository` —
are used consistently for component scanning and for the role each class
plays.

## 3. Database design

The database is PostgreSQL 16 and the schema is managed by Flyway, with
`V1__init_schema.sql` defining every table, index, and constraint that the
application relies on. There are six tables: `users` and `roles` (joined by
`user_roles`), `charging_stations`, `connectors`, `bookings`, and the
Spring Session JDBC tables (auto-created by Spring Session) used to persist
HTTP sessions across application restarts and across instances.

`charging_stations` carries the descriptive data the brief specifies — name,
address, city, latitude, longitude — and is one-to-many with `connectors`.
A `connector` row records its `connector_type` (TYPE2, CCS, CHADEMO or TESLA),
its `power_kw`, its `price_per_hour`, and an `active` flag. `bookings` joins
`users` and `connectors`, stores `start_time` and `end_time` as `timestamptz`,
and tracks a `status` of CONFIRMED, CANCELLED, or COMPLETED.

Two pieces of the schema deserve specific mention because they are central to
correctness:

1. A `CHECK (end_time > start_time)` constraint on `bookings` rejects any row
   whose times are out of order before the application can ever observe it.
2. A PostgreSQL `EXCLUDE USING gist` constraint, scoped to CONFIRMED
   bookings only, rejects any pair of overlapping reservations on the same
   connector. The constraint uses the `btree_gist` extension and a `tstzrange`
   expression to compare row intervals. This is the database's last line of
   defence against double booking — see Section 6.

Indexes are placed where the production workload demands them: a partial index
on `(connector_id, start_time, end_time) WHERE status='CONFIRMED'` accelerates
the overlap query that drives booking creation, and a composite index on
`(user_id, start_time)` accelerates the driver's own-bookings page.

## 4. Client-side design

The user interface is built with Thymeleaf for server-rendered pages and
Bootstrap 5 for layout and styling. A shared `fragments/layout.html` template
provides the navbar (with Spring-Security-aware `sec:authorize` directives),
the head, and the footer. Dynamic content — the station map and the booking
availability grid — is rendered in the browser via plain JavaScript that calls
the application's REST endpoints. The same `/api/v1/...` URIs serve both the
in-app JavaScript and any external REST client.

The map uses Leaflet against OpenStreetMap tiles. This combination was chosen
because it avoids the API-key and rate-limit overhead of Google Maps and is
appropriate for a public charging-station UI. Markers are placed for each
station returned by `GET /api/v1/stations`, and each popup links to the
station detail page. The detail page itself shows the station's connectors in
a table, with a "Book" action that takes the driver to a slot-picker.

The slot-picker is the most interactive piece of UI in the project. It calls
`GET /api/v1/connectors/{id}/availability?date=YYYY-MM-DD`, receives a list of
30-minute slots tagged as free, booked, or past, and renders them as a grid of
clickable tiles. The driver selects a contiguous range of free tiles, and the
form submits the resulting start and end as a standard `POST /bookings`. The
server validates everything again — alignment, ordering, future, conflict —
and either commits the booking or returns a clear error.

## 5. Authentication and RBAC

Authentication is handled by Spring Security 6, configured in a single
`SecurityConfig` class. The login form is a standard `formLogin` flow against
a `CustomUserDetailsService` that loads users from the `users` table; passwords
are stored as BCrypt hashes and verified by `DaoAuthenticationProvider`.

Role-based access control is enforced at two levels. URL-level rules in the
security filter chain ensure that `/admin/**` and `/api/v1/users/**` require
`ROLE_ADMIN`, while `/dashboard/**`, `/bookings/**`, and most of
`/api/v1/bookings/**` simply require an authenticated user. A second layer of
`@PreAuthorize` annotations on controller and service methods enforces the
more fine-grained "drivers can only see and modify their own bookings" rule:
the service layer compares the booking's `user_id` against the principal's id
and throws `ForbiddenOperationException` on mismatch. This means a driver who
constructs a URL targeting another driver's booking ID receives a 403, not a
silent success.

Two authentication entry points are configured so the user experience differs
between humans and machines: unauthenticated browser requests are redirected
to the login form, while unauthenticated `/api/**` requests receive a clean
HTTP 401. Access-denied responses for browser pages render a styled
`/error/403` page rather than the default whitelabel error.

## 6. Concurrent booking protection

Reservation systems live or die by their handling of concurrent writes. The
naive sequence — read availability, then insert — is a textbook race that
allows two transactions to both observe an empty slot and both insert a
booking. The platform defends against this with three layers, in order of
prevention:

1. **Pessimistic row lock.** `BookingService.createBooking` opens a single
   `READ_COMMITTED` transaction and calls `ConnectorRepository.findByIdForUpdate`,
   which issues a `SELECT ... FOR UPDATE` on the connector row. While the
   transaction holds that row lock, any other transaction trying to book the
   same connector must wait. The lock is acquired before the overlap query
   and held until commit, so the overlap check and the insert that follows it
   are effectively atomic with respect to other booking transactions.

2. **Overlap re-check inside the transaction.** With the connector row
   locked, the service runs a JPQL query that returns any CONFIRMED booking
   whose time range intersects the proposed one. Because no other booking
   transaction can be running on this connector while the lock is held, the
   check is authoritative — nothing can sneak in between check and insert.

3. **Database `EXCLUDE` constraint.** Even if the application logic were ever
   bypassed (a future bug, a direct SQL command, a code path that forgot the
   lock), the `EXCLUDE USING gist` constraint on `bookings` would reject the
   insert. Spring translates the resulting `DataIntegrityViolationException`
   into a `BookingConflictException`, surfaced to the user as a clean error
   message.

This three-layer defence is verified by an integration test
(`BookingServiceConcurrencyTest`) that spawns 20 threads racing to book the
same slot. The test asserts that exactly one thread succeeds, the other 19
receive `BookingConflictException`, and the database ends with exactly one
CONFIRMED row. The test runs in under eight seconds against a real Postgres
and passes consistently.

The choice between pessimistic and optimistic locking was deliberate.
Optimistic concurrency relies on retry, which is awkward when the contention
is between human users (each retry has user-visible latency). Pessimistic
locking serialises booking creation on a single connector, which is the
narrowest possible critical section and never produces a surprise rollback.
For a system where the conflict rate is low but the cost of a missed conflict
is high, this is the right trade.

## 7. Cloud deployment

The application is deployed to Heroku using the platform's Java buildpack.
Deployment is `git push heroku main`; the buildpack runs `./mvnw package`,
produces an executable jar, and starts it under the `web` dyno configured by
the project's `Procfile`. A `system.properties` file pins the runtime to
Java 17 so the buildpack provisions the correct JDK. Heroku Postgres is
attached as an add-on; the buildpack exports `JDBC_DATABASE_URL`,
`JDBC_DATABASE_USERNAME`, and `JDBC_DATABASE_PASSWORD` from the
add-on-managed `DATABASE_URL`, which the production `application-prod.yml`
consumes directly. Heroku Postgres requires SSL, which is enabled via a
Hikari `data-source-properties.sslmode=require` setting.

On startup the application runs Flyway migrations against the live database,
creating tables on first deploy and skipping them on subsequent ones. Spring
Session JDBC creates its own session tables on first start. The
`AdminBootstrap` component creates the initial administrator account from
`APP_ADMIN_BOOTSTRAP_EMAIL` / `_PASSWORD` environment variables, but only if
no admin user already exists — it is safe to leave set across restarts.

A request-logging filter, registered with `OncePerRequestFilter` and the
highest precedence, logs every request with the fields the brief enumerates:
ISO timestamp, HTTP method, requested URI, response status code, processing
time in milliseconds, and an instance identifier. The instance identifier
defaults to the `HOSTNAME` environment variable (which on Heroku is the dyno
name, e.g. `web.1`); it can be overridden by `APP_INSTANCE_ID` for non-Heroku
hosts. The filter sits outside any controller and works identically for
Thymeleaf pages and JSON endpoints, so every request is captured.

## 8. Externalised configuration

The application is configured exclusively through environment variables in
production. `application.yml` provides defaults and shared settings;
`application-dev.yml` is loaded only when `SPRING_PROFILES_ACTIVE=dev` and
contains developer convenience defaults (verbose SQL logging, demo data
seeding); `application-prod.yml` reads every connection-sensitive value
(`JDBC_DATABASE_URL`, credentials, admin bootstrap email and password) from
environment variables and provides no defaults that could accidentally leak.
No secrets are stored in source control: `.gitignore` excludes `.env`,
`.env.local`, and `application-local.yml`. The Heroku app configuration is
applied with `heroku config:set` and is owned by the platform, not the
codebase.

This separation lets the same compiled artefact run anywhere — a local
machine, a Render service, a Heroku dyno, a Kubernetes pod — by changing
environment variables alone.

## 9. Multi-instance execution

The deployed application is built to run as multiple identical instances. The
JVM holds no in-process state that another instance would need to share: HTTP
sessions are persisted in the database via Spring Session JDBC, so a user's
session survives a dyno restart and is honoured by whichever dyno serves the
next request. Authentication is therefore stateless from any individual
instance's point of view — the session row in the database is the source of
truth.

Database schema initialisation is also multi-instance safe. Flyway acquires
an advisory lock on the schema history table before applying migrations, so
when two instances boot simultaneously after a deploy, only one runs the
migration and the other waits and observes the result. The startup admin
bootstrap is idempotent by construction — it checks for an existing admin
before creating one.

The instance identifier in the request log makes multi-instance behaviour
observable: `heroku logs --tail` shows interleaved lines tagged with the
serving dyno's name, so it is trivial to confirm that load is reaching every
instance and that each request is handled by exactly one. A representative
production log line from the deployed instance reads:

```
ts=2026-05-29T18:29:32.743Z method=GET uri="/stations" status=200
duration_ms=84 instance=web.1
```

— containing every field the brief enumerates.

## 10. Key implementation challenges and decisions

**Lazy initialisation in Thymeleaf templates.** With
`spring.jpa.open-in-view=false` (a healthier default than the Spring Boot
out-of-the-box `true`), entity associations that are not eagerly fetched
inside the service cannot be navigated in templates — they raise
`LazyInitializationException`. The fix was to declare `@EntityGraph` overrides
on the repository methods that templates actually use (station with
connectors, booking with user/connector/station). This keeps a clean view
layer without paying the cost of N+1 queries.

**Authentication entry points.** Spring Security's
`defaultAuthenticationEntryPointFor` registers an entry point as the global
default if no other is present. The first version of `SecurityConfig`
inadvertently used `HttpStatusEntryPoint(401)` for every request, including
browser routes. The fix was to register two entry points explicitly — the
401 entry point for `/api/**`, and a `LoginUrlAuthenticationEntryPoint` for
`AnyRequestMatcher.INSTANCE` — so browsers continue to receive a friendly
login-form redirect while REST clients get a status code.

**Tests against local Postgres.** The integration tests originally used
Testcontainers, which depends on Docker. To keep the test loop runnable on a
machine without Docker, `AbstractPostgresIT` was rewritten to point at a
locally-running Postgres database, with override hooks via `TEST_DB_URL`,
`TEST_DB_USERNAME`, and `TEST_DB_PASSWORD`. Tests reuse a single
`evbooking_test` database, generate unique entity names with
`System.nanoTime()` so re-runs do not collide, and the concurrency test
provisions a fresh connector per run so it cannot conflict with previous
runs. All 33 tests pass deterministically on this setup.

**The principal-type mismatch in security tests.** Spring Security's
`@WithMockUser` provides a `org.springframework.security.core.userdetails.User`
as the principal, but the application's controllers expect the project's own
`SecurityUser`. For tests that depend on the principal having an `id`, the
fix was to seed real users in `@BeforeAll` and authenticate via
`@WithUserDetails`, which loads the user through the production
`CustomUserDetailsService` and produces a genuine `SecurityUser`.

## 11. Conclusions

The platform delivers everything the brief asks for: a layered monolithic
Spring Boot server, a Thymeleaf and Leaflet client with a map, full DRIVER /
ADMIN role-based access control, concurrency-safe booking under a stress
test, and a Heroku deployment configured to run as multiple instances with
externalised configuration and per-request logging. Thirty-three integration
and unit tests pass, including a 20-thread booking race that exercises the
pessimistic lock and EXCLUDE constraint together.

The most interesting design decision was the three-layer defence against
double booking, which combines an in-transaction database row lock, a
re-checked overlap query, and a database-level EXCLUDE constraint. The most
visible payoff was operational simplicity: the same artefact runs locally
under `mvn spring-boot:run` and on Heroku under `git push heroku main`, with
the only difference being environment variables.

If the system were to grow, the obvious next steps would be to split the
booking and catalogue concerns into separate services (the package boundaries
already anticipate this), move from Heroku to Kubernetes with a horizontal
pod autoscaler driving the dyno-equivalent, and introduce a real payments
integration. The architecture supports all of these without rewriting the
core booking logic — which is the strongest argument for the modular monolith
shape that was chosen.

---

*Word count target: 2000–3000. Actual: ~2400 words.*
