package com.smartcharge.evbooking.web;

import com.smartcharge.evbooking.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class StationRestSmokeTest extends AbstractPostgresIT {

    @Autowired MockMvc mvc;

    @Test @WithAnonymousUser
    void stations_endpoint_returns_json_array() throws Exception {
        mvc.perform(get("/api/v1/stations").accept("application/json"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test @WithMockUser(roles = "DRIVER")
    void driver_cannot_create_station() throws Exception {
        mvc.perform(post("/api/v1/stations")
                .with(csrf())
                .contentType("application/json")
                .content("""
                    {"name":"X","address":"Y","city":"Z","latitude":53,"longitude":-1}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test @WithAnonymousUser
    void error_payload_for_unknown_station_is_structured() throws Exception {
        mvc.perform(get("/api/v1/stations/99999999"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("\"status\":404")));
    }

    // Convenience CSRF helper for MockMvc with Spring Security
    private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
    }
}
