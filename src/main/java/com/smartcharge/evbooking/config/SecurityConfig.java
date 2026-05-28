package com.smartcharge.evbooking.config;

import com.smartcharge.evbooking.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security configuration.
 *
 * <p>URL access rules:</p>
 * <ul>
 *   <li>Public: home, login, register, station browsing, REST GET station endpoints, static assets.</li>
 *   <li>{@code /admin/**} and admin REST: ROLE_ADMIN only.</li>
 *   <li>{@code /dashboard/**}, {@code /bookings/**}, booking REST: authenticated (driver or admin).</li>
 * </ul>
 *
 * <p>Method-level security ({@code @PreAuthorize}) is enabled so service methods
 * can enforce ownership checks for "own bookings only".</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register",
                                 "/css/**", "/js/**", "/images/**", "/webjars/**",
                                 "/favicon.ico", "/robots.txt",
                                 "/.well-known/**",
                                 "/error", "/error/**",
                                 "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/stations", "/stations/**").permitAll()
                .requestMatchers("/api/v1/stations/**", "/api/v1/connectors/*/availability").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/bookings", "/api/v1/bookings/**").authenticated()
                .requestMatchers("/dashboard/**", "/bookings/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"));
        // CSRF protection is enabled by default. Thymeleaf form POSTs receive
        // the token automatically via the Spring Security/Thymeleaf integration.

        return http.build();
    }
}
