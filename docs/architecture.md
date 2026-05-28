# Architecture & design rationale

This document explains the technical design of the EV charging station booking platform.
It is structured around the ten questions the project brief asks us to answer.

---

## 1. Why a monolithic architecture was chosen

The system is a coursework artefact with a small, well-bounded scope: stations,
connectors, users, and bookings. The data is tightly coupled — a booking is meaningless
without the connector, and the connector is meaningless without its parent station.
Reads and writes are highly transactional and benefit from running in a single database
transaction.

A monolith was chosen because:

- **Strong consistency is essential.** Booking creation must be atomic across reading
  availability, locking the connector, inserting the booking, and validating the EXCLUDE
  constraint. A single database transaction in a single process is the simplest
  correct implementation. Distributed transactions or saga patterns would add complexity
  and failure modes with no benefit for a system of this scale.
- **Operational simplicity.** One artefact to build, deploy, observe, and roll back.
  Heroku's single-process-per-dyno model matches the monolith naturally.
- **Lower latency.** No inter-service network hops for what is, semantically, one
  reservation transaction.
- **Solo-developer ergonomics.** Refactoring across a single codebase is fast.
  Microservice boundaries are valuable when teams scale independently — that is not the
  shape of this project.

Microservices are a deliberate non-goal; they would over-engineer the artefact.
The brief explicitly mentioned monolithic architecture, and the system's complexity does
not justify a distributed one.

## 2. How the monolith is internally modular

The package layout enforces clean layering. Each Java package corresponds to one
architectural responsibility:

```
com.smartcharge.evbooking
├── domain          ← JPA entities (User, Role, ChargingStation, Connector, Booking)
├── repository      ← Spring Data JPA interfaces (only DB-talking code)
├── service         ← Business logic and transactions (UserService, BookingService, ...)
│   └── exception   ← Domain-specific exceptions
├── web
│   ├── controller  ← Thymeleaf @Controller classes for server-rendered pages
│   ├── rest        ← @RestController classes for JSON endpoints
│   ├── dto         ← Request / response / form objects (no JPA entities cross this line)
│   ├── mapper      ← Entity ↔ DTO translation
│   └── advice      ← @ControllerAdvice / @RestControllerAdvice exception handlers
├── security        ← UserDetailsService adapter + Spring Security plumbing
├── config          ← @Configuration beans (security, clock, Heroku data source)
└── infrastructure  ← Cross-cutting concerns (logging filter, seeders)
```

The rules are enforced by convention but easy to verify:

- Controllers never call repositories; they call services.
- Services never produce DTOs or talk to HTTP; they return entities and throw domain
  exceptions.
- Entities never escape the controller boundary; controllers map them through
  `StationMapper` / `BookingMapper` before returning.
- Cross-cutting concerns (request logging, exception handling) live as filters and
  advices, not inside controllers.

This is what makes the project a "modular monolith" rather than a ball of mud. Each
package could, in principle, become a microservice without redesigning the layers.

## 3. How MVC is used

The codebase is a textbook Spring MVC application:

- **Model.** JPA entities under `domain` and request/response DTOs under `web.dto`.
  Thymeleaf templates read model attributes populated by controllers.
- **View.** Thymeleaf templates under `src/main/resources/templates`. The shared
  `fragments/layout.html` provides navbar, head, and footer fragments via the
  Thymeleaf `~{...}` fragment syntax.
- **Controller.** `@Controller` classes return logical view names; `@RestController`
  classes return objects that Jackson serialises to JSON. Both inherit Spring MVC's
  dispatcher mechanics — request mapping, content negotiation, validation, exception
  resolution.

Where the project deviates from pure server-rendered MVC: dynamic UI (the Leaflet map,
the availability grid, AJAX booking confirmations) is implemented as JS in
`src/main/resources/static/js`, consuming the same `/api/v1/...` REST endpoints that an
external client could use. This gives us the best of both worlds — SEO-friendly,
session-aware server-rendered pages where they matter, REST where the UI needs it.

## 4. How IoC / Dependency Injection is used

Every collaborator is supplied via **constructor injection**:

```java
@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ConnectorRepository connectorRepository;
    private final BookingPolicy policy;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository,
                          ConnectorRepository connectorRepository,
                          BookingPolicy policy,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        ...
    }
}
```

The Spring container instantiates each bean once at startup (default scope: singleton)
and wires it into anything that declares a constructor parameter of the right type.
Benefits:

- **Testability.** Tests instantiate services with mocks or fakes (e.g. a fixed `Clock`)
  with no framework involvement.
- **Immutability.** All dependencies are `private final` and cannot be swapped at
  runtime.
- **Explicit dependencies.** A class's constructor parameter list is a precise contract
  of what it needs to function — no hidden `@Autowired` field setters, no service
  locator pattern.

Examples of Spring bean stereotypes used:

| Stereotype | Role |
|---|---|
| `@Service`       | `BookingService`, `StationService`, `UserService`, `AvailabilityService` |
| `@Repository`    | All Spring Data JPA repositories |
| `@Controller`    | All Thymeleaf controllers |
| `@RestController`| All JSON-API controllers |
| `@Component`     | `BookingPolicy`, `StationMapper`, `BookingMapper`, `RequestLoggingFilter`, `AdminBootstrap`, `DataSeeder` |
| `@Configuration` | `SecurityConfig`, `ClockConfig` |

Custom beans (e.g. `Clock systemUtcClock()` in `ClockConfig`) are exposed via `@Bean`
methods, allowing tests to override them.

## 5. How REST principles are reflected

The REST API under `/api/v1` is **resource-oriented**:

| Verb | URI | Meaning |
|---|---|---|
| `GET`    | `/api/v1/stations`                         | List stations (filterable) |
| `GET`    | `/api/v1/stations/{id}`                    | Get one station |
| `POST`   | `/api/v1/stations`                         | Create a station (ADMIN) |
| `PUT`    | `/api/v1/stations/{id}`                    | Replace station |
| `DELETE` | `/api/v1/stations/{id}`                    | Delete station |
| `POST`   | `/api/v1/stations/{id}/connectors`         | Add a connector to a station |
| `PUT`    | `/api/v1/connectors/{id}`                  | Replace connector |
| `DELETE` | `/api/v1/connectors/{id}`                  | Delete connector |
| `GET`    | `/api/v1/connectors/{id}/availability?date=…` | Free slots for a day |
| `POST`   | `/api/v1/bookings`                         | Create a booking |
| `GET`    | `/api/v1/bookings/me`                      | Own bookings |
| `GET`    | `/api/v1/bookings`                         | All bookings (ADMIN) |
| `PATCH`  | `/api/v1/bookings/{id}`                    | Modify own future booking |
| `DELETE` | `/api/v1/bookings/{id}`                    | Cancel own (or any, if ADMIN) |

Adherence to REST principles:

- **Nouns in URIs, verbs in HTTP methods.** No `/createStation` or `/cancelBookingById`.
- **Status codes carry semantics.** `201 Created` for `POST`, `204 No Content` for
  `DELETE`, `404` for unknown resources, `409` for overlap conflicts, `403` for RBAC
  violations, `400` for validation errors. Each is produced by the dedicated handler in
  `RestExceptionHandler`.
- **Stateless requests.** Each request carries either the session cookie or no
  authentication. Server state lives in the database, not in memory — see §9.
- **Consistent error shape.**
  ```json
  { "timestamp": "...", "status": 409, "error": "Conflict",
    "message": "The selected time slot conflicts with...", "path": "/api/v1/bookings" }
  ```
- **`Location` header on creation.** `POST /api/v1/bookings` returns
  `Location: /api/v1/bookings/{id}` so the client can refetch.
- **Versioned prefix.** `/api/v1` reserves room for breaking changes later.

## 6. How authentication and role-based access work

Spring Security 6 is configured in `SecurityConfig`:

- **Authentication** — form login at `/login`. `CustomUserDetailsService` loads users
  by email, returning a `SecurityUser` (an adapter wrapping the JPA `User`). Passwords
  are hashed with BCrypt via `PasswordEncoder`. No password ever leaves the database in
  cleartext.
- **Authorisation** at two layers:
  - **URL-level**, via `authorizeHttpRequests`: `/admin/**` requires `ROLE_ADMIN`,
    `/dashboard/**` requires authentication, public stations remain reachable.
  - **Method-level**, via `@PreAuthorize("hasRole('ADMIN')")` on REST and admin
    controllers. This adds defence-in-depth — even if a URL rule were misconfigured,
    the method check still fires.
  - **Ownership checks** inside `BookingService`. Drivers may only modify/cancel their
    own bookings; admins may act on any. The check is enforced in the service layer
    rather than the controller because the service is the only layer that has the
    domain knowledge (`booking.isOwnedBy(userId)`).
- **CSRF protection** is on by default for form posts. The Thymeleaf template embeds
  the CSRF token in a `<meta>` tag and the JS layer sends it as `X-XSRF-TOKEN` for the
  REST calls it issues.
- **Sessions** are persisted via Spring Session JDBC (see §9 and §10).

The two configured roles match the brief:

- `ROLE_DRIVER` — can browse, book, view and cancel own bookings.
- `ROLE_ADMIN` — can do everything plus manage stations, connectors, and any booking.

The bootstrap admin is created on first start via env-var-driven credentials (see
`AdminBootstrap`), so there is never a hard-coded password in the codebase.

## 7. How PostgreSQL persistence works

- **Spring Data JPA** with Hibernate as provider. Each entity maps directly to a table
  via `@Entity` / `@Table`.
- **Schema is owned by Flyway**, not by Hibernate. JPA is configured with
  `ddl-auto=validate`, so Hibernate verifies the schema matches the entities but never
  changes it. The single source of truth is `db/migration/V1__init_schema.sql`. Future
  changes live as `V2__*.sql`, `V3__*.sql`, etc.
- **Connection pool.** HikariCP, configured for sane defaults; size is tunable via
  `DB_POOL_MAX` / `DB_POOL_MIN` env vars.
- **Time storage.** All timestamps are `TIMESTAMPTZ` and Hibernate is configured to use
  UTC (`jdbc.time_zone=UTC`). The app does not assume any client time zone.
- **Postgres-specific features used:**
  - `btree_gist` extension and the `EXCLUDE` constraint on bookings — see §8.
  - `NUMERIC(p,s)` for prices and coordinates (no floating-point money).
  - `CHECK` constraints for status enum and coordinate ranges.

## 8. How booking concurrency is handled

This is the safety-critical piece of the system, and it uses **three layers of
defence** so that a bug in any one layer cannot cause double-booking.

### The hazard

Naive "check then insert" has a race:

```
  T1: SELECT overlaps           -> none
                                       T2: SELECT overlaps      -> none
  T1: INSERT booking [10:00,10:30]
                                       T2: INSERT booking [10:00,10:30]   ← both succeed
```

Without protection, this scenario is not just possible — under load it is *expected*.

### Layer 1 — Pessimistic row lock

`BookingService.createBooking` opens a transaction (READ_COMMITTED), then calls
`connectorRepository.findByIdForUpdate(connectorId)`, which issues
`SELECT ... FOR UPDATE` on the `connectors` row. Any other transaction trying to lock
the same row blocks until ours commits or rolls back. This serialises the booking flow
*on a per-connector basis* — different connectors continue to book in parallel.

### Layer 2 — Overlap re-check inside the locked transaction

With the lock held, `bookingRepository.findOverlapping(...)` is executed. Because no
concurrent booker can hold the same connector lock, this check is authoritative:
nothing can insert a competing row between our read and our write. If an overlap is
found we throw `BookingConflictException` (HTTP 409).

### Layer 3 — Database EXCLUDE constraint

The migration installs:

```sql
ALTER TABLE bookings ADD CONSTRAINT no_overlapping_confirmed_bookings
EXCLUDE USING gist (
    connector_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
) WHERE (status = 'CONFIRMED');
```

This says, in SQL: "no two CONFIRMED rows may share a `connector_id` and have
overlapping time ranges." If a row that violates the rule is ever inserted — via the
application, via a direct SQL prompt, via a future code refactor that forgets the
lock — Postgres rejects it. The service translates the resulting
`DataIntegrityViolationException` into the same `BookingConflictException` the user
sees, so the contract is preserved.

### Why this is robust across multiple application instances

Both the lock and the constraint live in the database, not in the application. If
Heroku spawns multiple dynos and two users on different dynos race for the same slot,
they still queue on the same Postgres row lock and still face the same EXCLUDE
constraint.

### What is tested

- `BookingServiceConcurrencyTest` spawns 20 threads racing the same slot and asserts
  that **exactly one** booking succeeds and **all others** receive
  `BookingConflictException`.
- `BookingOverlapRepositoryTest` exercises every overlap edge case (exact, contained,
  straddling start, straddling end, back-to-back, cancelled rows ignored, modify
  excludes self) and asserts the EXCLUDE constraint also rejects a direct overlapping
  insert.

## 9. How the app is cloud-ready

Cloud-readiness is achieved by following the relevant *twelve-factor* practices:

- **Externalised configuration.** Every environment-specific value comes from
  environment variables: `DATABASE_URL`, `SPRING_DATASOURCE_*`,
  `APP_ADMIN_BOOTSTRAP_*`, `JAVA_OPTS`, `PORT`, `APP_INSTANCE_ID`. No secrets are ever
  committed to the repo. Spring profiles (`dev`, `prod`) select profile-specific YAML.
- **Stateless application processes.** Booking state, user state, and even HTTP session
  state live in Postgres (via Spring Session JDBC). Any dyno can serve any request.
- **Single artefact, immutable build.** A fat jar produced by Spring Boot is the unit of
  deployment; `Dockerfile` and `Procfile` are alternative ways to launch it.
- **Logs to stdout.** Spring Boot's default console logger streams to stdout, which
  Heroku's log router collects automatically. Each line carries the dyno id, satisfying
  the assignment's logging requirement.
- **Disposable processes.** Startup runs Flyway migrations idempotently; shutdown
  finishes in-flight requests via Spring Boot's graceful shutdown.
- **Backing services as attached resources.** Postgres is bound via `DATABASE_URL`,
  detached and reattached freely without code changes.
- **Health check endpoint.** `/actuator/health` is exposed publicly so the platform can
  detect a broken dyno.

## 10. How it could evolve into microservices or Kubernetes

The brief asks for the future scaling story even though we do not implement it. Here it is:

### Possible service split

If the system grew, the natural seams are already visible:

- **Catalogue service** — `ChargingStation`, `Connector` entities and their REST
  endpoints. Mostly read-heavy and can sit behind aggressive caching.
- **Booking service** — `Booking` entity and `BookingService`. Owns the concurrency
  logic and the EXCLUDE constraint. Would need its own database; cross-service
  references to connectors would become IDs validated asynchronously (event-driven
  consistency).
- **Identity service** — `User`, `Role`, authentication. Could expose OIDC and be
  replaced by a managed identity provider.

The current package layout (`domain`, `service`, `web`, `repository`) means a split
would be largely about extracting whole packages and replacing in-process method calls
with HTTP or message-bus calls.

### Possible Kubernetes deployment

- A single `Deployment` resource managing replica pods (replaces Heroku dynos).
- A `Service` of type `ClusterIP` plus an `Ingress` controller for public traffic.
- A managed Postgres outside the cluster, accessed via a `Secret` for credentials.
- `ConfigMap` for non-secret env vars; `HorizontalPodAutoscaler` for load-based scaling.
- Rolling updates by default; readiness/liveness probes hit `/actuator/health`.

What does *not* change when moving to Kubernetes:

- Statelessness, externalised configuration, and the database-level concurrency
  protection all continue to work without modification.
- The same `Dockerfile` produces the same image.

What *would* need attention:

- Spring Session JDBC limits horizontal scale to roughly what one Postgres instance can
  serve; for very large fleets a dedicated Redis-backed session store is more
  appropriate.
- Distributed tracing (e.g. OpenTelemetry → Tempo/Jaeger) becomes more valuable than
  the simple per-instance request log.

Kubernetes is the right tool when you need orchestration of many services, rolling
upgrades coordinated across them, or auto-scaling beyond what a PaaS exposes. For this
project, Heroku is the right level of abstraction.

---

## Pages and endpoints (reference)

### Server-rendered pages

| Path | Method | Access |
|---|---|---|
| `/` | GET | public |
| `/login`, `/register` | GET/POST | public |
| `/logout` | POST | authenticated |
| `/stations`, `/stations/{id}` | GET | public |
| `/bookings/new`, `/bookings` | GET/POST | authenticated |
| `/dashboard`, `/dashboard/bookings`, `/dashboard/bookings/{id}/cancel` | GET/POST | authenticated |
| `/admin`, `/admin/stations/**`, `/admin/connectors/**`, `/admin/bookings/**`, `/admin/users` | GET/POST | ADMIN |

### REST API

| Method | Path | Access |
|---|---|---|
| GET    | `/api/v1/stations`                         | public |
| GET    | `/api/v1/stations/{id}`                    | public |
| POST   | `/api/v1/stations`                         | ADMIN |
| PUT    | `/api/v1/stations/{id}`                    | ADMIN |
| DELETE | `/api/v1/stations/{id}`                    | ADMIN |
| POST   | `/api/v1/stations/{id}/connectors`         | ADMIN |
| PUT    | `/api/v1/connectors/{id}`                  | ADMIN |
| DELETE | `/api/v1/connectors/{id}`                  | ADMIN |
| GET    | `/api/v1/connectors/{id}/availability`     | public |
| POST   | `/api/v1/bookings`                         | authenticated |
| GET    | `/api/v1/bookings/me`                      | authenticated |
| PATCH  | `/api/v1/bookings/{id}`                    | owner or ADMIN |
| DELETE | `/api/v1/bookings/{id}`                    | owner (future only) or ADMIN |
| GET    | `/api/v1/bookings`                         | ADMIN |
