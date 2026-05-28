# Testing plan

The test layer is structured by priority. The booking concurrency proof is the
headline test; security and overlap edge cases come next; controller smoke tests
provide breadth.

## What is tested, and where

### 1. Booking overlap validation
- `service/BookingPolicyTest` — pure unit tests covering: slot alignment,
  start/end ordering, equal start/end rejection, past rejection, max-advance rejection,
  max-duration rejection, single-slot acceptance, max-length acceptance.
- `repository/BookingOverlapRepositoryTest` — Testcontainers Postgres tests for every
  overlap case (exact, contained, straddling start/end, back-to-back, cancelled rows,
  modify-excludes-self) AND the database EXCLUDE constraint rejecting a direct overlap.

### 2. Concurrency
- `service/BookingServiceConcurrencyTest` — 20 parallel threads racing for the
  same slot. Asserts exactly one CONFIRMED row results and the other 19 see
  `BookingConflictException`. No other exceptions allowed.

### 3. Ownership and modification rules
- `service/BookingServiceOwnershipTest` —
  - Driver A cannot cancel Driver B's booking.
  - Admin can cancel any booking.
  - Driver A cannot modify Driver B's booking.
  - Nobody can modify a booking whose start time has passed.

### 4. Security / role access
- `security/SecurityRulesTest` —
  - Anonymous: home and stations OK; dashboard and admin redirect to login.
  - Driver: dashboard OK, admin returns 403.
  - Admin: admin dashboard OK.
  - Unauthenticated API access to admin-only or member-only endpoints returns 401.

### 5. Controller smoke tests
- `web/StationRestSmokeTest` — JSON list endpoint returns 200, admin-only POST blocked
  for driver role, 404 produces the structured error body.

## How tests are wired

- `AbstractPostgresIT` spins up a single shared Postgres 16 container via Testcontainers
  for the entire JVM, and contributes its JDBC URL/credentials to the Spring context via
  an `ApplicationContextInitializer`.
- Repository tests use `@DataJpaTest` with `replace=NONE` so Flyway runs against the
  real container.
- Service and security tests use `@SpringBootTest` to load the full context.

## Running

```bash
# All tests (requires Docker for Testcontainers)
mvn test

# Only fast unit tests
mvn test -Dtest=BookingPolicyTest
```

## Manual test checklist

Before submitting, verify in a browser against the deployed app:

1. Anonymous user can browse `/stations` and see the map.
2. Anonymous user is redirected from `/dashboard` to `/login`.
3. Register a new driver → redirect to login → log in.
4. From `/stations`, open a station and create a booking. Verify it appears in
   `/dashboard/bookings`.
5. Try to book the same slot again from another browser session → expect a 409 / error
   message.
6. Cancel a future booking; refresh — gone from upcoming list.
7. Log in as bootstrap admin → `/admin` loads → create a new station via
   `/admin/stations/new`. Verify it appears for drivers.
8. Admin cancels a driver's booking; the driver sees it as CANCELLED.
9. Check `heroku logs --tail` for access log lines with all six required fields.

## What is intentionally NOT tested in code

- The Leaflet map UI (covered by manual test).
- The Heroku `DATABASE_URL` parser (verified by an actual Heroku deployment;
  introducing a test would require mocking the environment post-processor which adds
  fragile machinery for little gain).
- Visual regression on Thymeleaf templates.

## Coverage target

Critical-path coverage targets:
- `BookingService.createBooking` — every branch, including the
  `DataIntegrityViolationException` fallback.
- `BookingPolicy.validateRange` — every rejection reason.

Overall line coverage is not a primary goal for an academic project; correctness on the
booking flow is.
