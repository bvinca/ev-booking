-- =====================================================================
-- V1__init_schema.sql
-- Baseline schema for the EV charging station booking platform.
-- =====================================================================

-- Required for the EXCLUDE constraint on bookings (range overlap detection).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- Users & roles
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id   SMALLSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- ---------------------------------------------------------------------
-- Charging stations & connectors
-- ---------------------------------------------------------------------
CREATE TABLE charging_stations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(140) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    latitude    NUMERIC(9,6) NOT NULL CHECK (latitude  BETWEEN -90  AND 90),
    longitude   NUMERIC(9,6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_stations_city ON charging_stations (city);

CREATE TABLE connectors (
    id              BIGSERIAL PRIMARY KEY,
    station_id      BIGINT       NOT NULL REFERENCES charging_stations(id) ON DELETE CASCADE,
    connector_type  VARCHAR(30)  NOT NULL,
    power_kw        NUMERIC(5,1) NOT NULL CHECK (power_kw > 0),
    price_per_hour  NUMERIC(6,2) NOT NULL CHECK (price_per_hour >= 0),
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_connectors_station ON connectors (station_id);

-- ---------------------------------------------------------------------
-- Bookings
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id),
    connector_id BIGINT      NOT NULL REFERENCES connectors(id),
    start_time   TIMESTAMPTZ NOT NULL,
    end_time     TIMESTAMPTZ NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_booking_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_booking_status     CHECK (status IN ('CONFIRMED','CANCELLED','COMPLETED'))
);
CREATE INDEX idx_bookings_user_time      ON bookings (user_id, start_time);
CREATE INDEX idx_bookings_connector_time ON bookings (connector_id, start_time, end_time)
    WHERE status = 'CONFIRMED';

-- Defence-in-depth: PostgreSQL EXCLUDE constraint preventing any two
-- CONFIRMED bookings for the same connector from overlapping in time.
-- This protects against any application-level race that might slip through.
ALTER TABLE bookings
    ADD CONSTRAINT no_overlapping_confirmed_bookings
    EXCLUDE USING gist (
        connector_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED');

-- ---------------------------------------------------------------------
-- Seed roles
-- ---------------------------------------------------------------------
INSERT INTO roles (name) VALUES ('ROLE_DRIVER'), ('ROLE_ADMIN');
