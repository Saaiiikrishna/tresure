package com.treasurehunt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.InvalidSessionStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security configuration
 * Configures authentication and authorization for the admin panel
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.security.admin.username}")
    private String adminUsername;

    @Value("${app.security.admin.password}")
    private String adminPassword;

    private static final String CSP_POLICY =
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://www.youtube.com; " +
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' https://cdnjs.cloudflare.com; " +
            "frame-src 'self' https://www.youtube.com https://youtube.com; " +
            "media-src 'self' https://www.youtube.com https://youtube.com;";

    /**
     * Configure HTTP security
     * @param http HttpSecurity configuration
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - Home and API
                .requestMatchers("/", "/home", "/index").permitAll()
                .requestMatchers("/api/plans/**", "/api/register", "/api/register/**", "/api/health").permitAll()
                .requestMatchers("/register/form/**").permitAll() // Registration form fragments

                // Static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/robots.txt").permitAll()
                .requestMatchers("/secure/files/images/**").permitAll() // Public images through secure controller

                // Public pages
                .requestMatchers("/about", "/contact", "/privacy", "/terms").permitAll()
                .requestMatchers("/error", "/error/**").permitAll()

                // Health checks
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Admin endpoints - require authentication
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/images/**").hasRole("ADMIN")
                .requestMatchers("/secure/files/documents/**").hasRole("ADMIN") // Secure document access

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin", false) // Changed to false to allow redirect to original URL
                .failureUrl("/admin/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionStrategy(customInvalidSessionStrategy())
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/register", "/api/register/**", "/api/plans/**", "/api/health"
                ) // Only public registration API endpoints are CSRF-exempt; admin endpoints require CSRF tokens
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY))
            );

        return http.build();
    }

    /**
     * Configure in-memory user details service
     * @return UserDetailsService with admin user
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Password encoder bean
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Custom invalid session strategy that doesn't redirect API requests
     * @return InvalidSessionStrategy
     */
    @Bean
    public InvalidSessionStrategy customInvalidSessionStrategy() {
        return new InvalidSessionStrategy() {
            @Override
            public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response) throws IOException {
                String requestURI = request.getRequestURI();

                // Don't redirect API requests - just return 401
                if (requestURI.startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid or expired session\",\"status\":401}");
                    return;
                }

                // For non-API requests, redirect to home
                response.sendRedirect("/");
            }
        };
    }
}
