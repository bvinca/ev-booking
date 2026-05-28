package com.smartcharge.evbooking.infrastructure.seed;

import com.smartcharge.evbooking.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On startup, if no admin exists and bootstrap credentials are configured
 * via environment variables, create the first admin user. Idempotent.
 */
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminBootstrap(UserService userService,
                          @Value("${app.admin-bootstrap.email:}") String email,
                          @Value("${app.admin-bootstrap.password:}") String password,
                          @Value("${app.admin-bootstrap.full-name:System Administrator}") String fullName) {
        this.userService = userService;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        if (userService.countAdmins() > 0) {
            log.debug("Admin user already exists; skipping bootstrap.");
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No admin in DB and APP_ADMIN_BOOTSTRAP_* env vars not set — skipping admin bootstrap. " +
                "You can manually promote a user later with: " +
                "UPDATE user_roles SET role_id = (SELECT id FROM roles WHERE name='ROLE_ADMIN') WHERE user_id = <id>;");
            return;
        }
        userService.registerAdmin(fullName, email, password);
        log.info("Bootstrap admin created for email={}", email);
    }
}
