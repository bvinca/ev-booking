package com.smartcharge.evbooking.security;

import com.smartcharge.evbooking.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class SecurityRulesTest extends AbstractPostgresIT {

    @Autowired MockMvc mvc;

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

    @Test @WithMockUser(roles = "DRIVER")
    void driver_cannot_access_admin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "DRIVER")
    void driver_can_access_dashboard() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void admin_can_access_admin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }

    @Test @WithAnonymousUser
    void api_admin_endpoints_are_protected() throws Exception {
        mvc.perform(get("/api/v1/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }
}
