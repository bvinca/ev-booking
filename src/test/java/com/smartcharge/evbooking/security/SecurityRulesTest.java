package com.smartcharge.evbooking.security;

import com.smartcharge.evbooking.AbstractPostgresIT;
import com.smartcharge.evbooking.service.UserService;
import com.smartcharge.evbooking.service.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * URL-level access rules.
 *
 * <p>For tests that depend on a real {@link com.smartcharge.evbooking.security.SecurityUser}
 * principal (so controllers can call {@code principal.getId()}), we seed real users in
 * {@link #seedTestUsers()} and authenticate via {@link WithUserDetails}, which goes
 * through our {@link com.smartcharge.evbooking.security.CustomUserDetailsService}.</p>
 *
 * <p>For pure URL-access boundary checks (anonymous vs role-restricted),
 * {@link WithMockUser} is sufficient — the request is rejected before any controller runs.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class SecurityRulesTest extends AbstractPostgresIT {

    static final String DRIVER_EMAIL = "driver.security-test@example.com";
    static final String ADMIN_EMAIL  = "admin.security-test@example.com";

    @Autowired MockMvc mvc;
    @Autowired UserService userService;

    @BeforeAll
    void seedTestUsers() {
        try { userService.registerDriver("Sec Test Driver", DRIVER_EMAIL, "password123"); }
        catch (EmailAlreadyUsedException ignored) { /* idempotent across reruns */ }
        try { userService.registerAdmin("Sec Test Admin", ADMIN_EMAIL, "password123"); }
        catch (EmailAlreadyUsedException ignored) { /* idempotent across reruns */ }
    }

    // ---- Anonymous user ----

    @Test @WithAnonymousUser
    void anonymous_can_view_home() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test @WithAnonymousUser
    void anonymous_can_view_stations() throws Exception {
        mvc.perform(get("/stations")).andExpect(status().isOk());
    }

    @Test @WithAnonymousUser
    void anonymous_is_redirected_from_dashboard() throws Exception {
        mvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test @WithAnonymousUser
    void anonymous_is_redirected_from_admin() throws Exception {
        mvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test @WithAnonymousUser
    void api_admin_endpoints_are_protected() throws Exception {
        mvc.perform(get("/api/v1/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    // ---- Role boundary checks (mock principal sufficient — request blocked before controller) ----

    @Test @WithMockUser(roles = "DRIVER")
    void driver_cannot_access_admin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    // ---- Authenticated dashboards (need real SecurityUser principal) ----

    @Test @WithUserDetails(DRIVER_EMAIL)
    void driver_can_access_dashboard() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isOk());
    }

    @Test @WithUserDetails(ADMIN_EMAIL)
    void admin_can_access_admin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }
}
