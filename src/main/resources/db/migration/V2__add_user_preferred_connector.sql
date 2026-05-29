-- =====================================================================
-- V2__add_user_preferred_connector.sql
-- Lets drivers set a preferred connector type that auto-applies as the
-- default filter on the station map and list pages.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN preferred_connector_type VARCHAR(30);

-- Validates against the same set used by the Connector table.
ALTER TABLE users
    ADD CONSTRAINT chk_users_preferred_connector
    CHECK (preferred_connector_type IS NULL
           OR preferred_connector_type IN ('TYPE2','CCS','CHADEMO','TESLA'));
