package com.smartcharge.evbooking.web;

import com.smartcharge.evbooking.AbstractPostgresIT;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.ConnectorType;
import com.smartcharge.evbooking.service.UserService;
import com.smartcharge.evbooking.service.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies:
 *   1. GET /dashboard/profile renders with the current preference visible.
 *   2. POST saves the preference and redirects back.
 *   3. POST with empty value clears the preference.
 *   4. Visiting /stations without ?type= auto-applies the saved preference.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = AbstractPostgresIT.Initializer.class)
class ProfileControllerTest extends AbstractPostgresIT {

    static final String DRIVER_EMAIL = "profile-test@example.com";

    @Autowired MockMvc mvc;
    @Autowired UserService userService;

    @BeforeAll
    void seed() {
        try {
            User u = userService.registerDriver("Profile Tester", DRIVER_EMAIL, "password123");
            // Reset preference so each run starts from a known state
            userService.updatePreferredConnector(u.getId(), null);
        } catch (EmailAlreadyUsedException ignored) {
            User u = userService.findByEmail(DRIVER_EMAIL);
            userService.updatePreferredConnector(u.getId(), null);
        }
    }

    @Test
    @WithUserDetails(DRIVER_EMAIL)
    void profile_page_renders() throws Exception {
        mvc.perform(get("/dashboard/profile"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard/profile"))
            .andExpect(model().attributeExists("user", "connectorTypes"));
    }

    @Test
    @WithUserDetails(DRIVER_EMAIL)
    void posting_a_preference_saves_and_clears() throws Exception {
        mvc.perform(post("/dashboard/profile")
                .with(csrf())
                .param("preferredConnectorType", "CCS"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard/profile"));

        User after = userService.findByEmail(DRIVER_EMAIL);
        assertThat(after.getPreferredConnectorType()).isEqualTo(ConnectorType.CCS);

        // Clear it
        mvc.perform(post("/dashboard/profile")
                .with(csrf())
                .param("preferredConnectorType", ""))
            .andExpect(status().is3xxRedirection());

        User cleared = userService.findByEmail(DRIVER_EMAIL);
        assertThat(cleared.getPreferredConnectorType()).isNull();
    }

    @Test
    @WithUserDetails(DRIVER_EMAIL)
    void stations_page_auto_applies_preference_when_no_type_param() throws Exception {
        User u = userService.findByEmail(DRIVER_EMAIL);
        userService.updatePreferredConnector(u.getId(), ConnectorType.CHADEMO);

        // No ?type= in URL → preference is applied automatically
        mvc.perform(get("/stations"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("type", ConnectorType.CHADEMO))
            .andExpect(model().attribute("defaultedFromProfile", true));

        // Explicit ?type= overrides (and "type=" alone means "Any")
        mvc.perform(get("/stations").param("type", ""))
            .andExpect(status().isOk())
            .andExpect(model().attribute("defaultedFromProfile", false));
    }
}
