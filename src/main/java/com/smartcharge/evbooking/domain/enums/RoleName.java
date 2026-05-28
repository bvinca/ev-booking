package com.smartcharge.evbooking.domain.enums;

/**
 * Roles supported by the platform. The string values match the {@code name}
 * column in the {@code roles} table and the authorities used by Spring Security
 * ({@code ROLE_*} prefix is preserved).
 */
public enum RoleName {
    ROLE_DRIVER,
    ROLE_ADMIN
}
