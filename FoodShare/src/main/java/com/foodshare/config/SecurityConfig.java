package com.foodshare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;
    private final String corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          Environment environment,
                          @Value("${CORS_ALLOWED_ORIGINS:${app.cors.allowed-origins:}}") String corsAllowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.environment = environment;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        if (isDev) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }

        http.authorizeHttpRequests(auth -> {
            // Static web pages and frontend assets
            auth.requestMatchers(
                "/", "/*.html", "/css/**", "/js/**", "/images/**", "/favicon.ico"
            ).permitAll();

            // Development-only H2 console
            if (isDev) {
                auth.requestMatchers("/h2-console/**").permitAll();
            }

            // Health and monitoring probes for container orchestrators
            auth.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();

            // Public Authentication APIs
            auth.requestMatchers("/api/auth/**").permitAll();

            // Route shadowing prevention: /api/listings/my MUST require authentication
            auth.requestMatchers(HttpMethod.GET, "/api/listings/my").hasAnyRole("PROVIDER", "ADMIN");

            // Public listing viewing and search (only available list and details)
            auth.requestMatchers(HttpMethod.GET, "/api/listings/available", "/api/listings/{id:[0-9]+}").permitAll();

            // Recipient operations
            auth.requestMatchers(HttpMethod.POST, "/api/requests/**").hasAnyRole("RECIPIENT", "ADMIN");
            auth.requestMatchers(HttpMethod.DELETE, "/api/requests/**").hasAnyRole("RECIPIENT", "ADMIN");

            // Prioritized request viewing restricted to Volunteers and Admins
            auth.requestMatchers("/api/requests/prioritized").hasAnyRole("VOLUNTEER", "ADMIN");

            // Provider operations
            auth.requestMatchers(HttpMethod.POST, "/api/listings/**").hasAnyRole("PROVIDER", "ADMIN");
            auth.requestMatchers(HttpMethod.PUT, "/api/listings/**").hasAnyRole("PROVIDER", "ADMIN");
            auth.requestMatchers(HttpMethod.DELETE, "/api/listings/**").hasAnyRole("PROVIDER", "ADMIN");
            auth.requestMatchers("/api/tokens/verify", "/api/tokens/confirm-collection").hasAnyRole("PROVIDER", "VOLUNTEER", "ADMIN");

            // Volunteer operations
            auth.requestMatchers("/api/volunteers/**").hasAnyRole("VOLUNTEER", "ADMIN");
            auth.requestMatchers("/api/distributions/camp").hasAnyRole("VOLUNTEER", "ADMIN");

            // User self-profile endpoint
            auth.requestMatchers("/api/users/profile").authenticated();

            // Admin user management endpoints
            auth.requestMatchers(HttpMethod.GET, "/api/users", "/api/users/").hasRole("ADMIN");
            auth.requestMatchers(HttpMethod.PUT, "/api/users/*/role").hasRole("ADMIN");

            // Common authenticated APIs
            auth.requestMatchers("/api/users/**", "/api/tokens/**", "/api/distributions/**").authenticated();
            auth.anyRequest().authenticated();
        });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new ArrayList<>();

        if (corsAllowedOrigins != null && !corsAllowedOrigins.trim().isEmpty()) {
            if ("*".equals(corsAllowedOrigins.trim())) {
                configuration.addAllowedOriginPattern("*");
            } else {
                for (String origin : corsAllowedOrigins.split(",")) {
                    String trimmed = origin.trim();
                    if (!trimmed.isEmpty()) {
                        origins.add(trimmed);
                    }
                }
                configuration.setAllowedOrigins(origins);
            }
        } else {
            // Always permit local development and common origins by default
            origins.add("http://localhost:8080");
            origins.add("http://localhost:8081");
            origins.add("http://localhost:8082");
            origins.add("http://localhost:8083");
            origins.add("http://localhost:8084");
            origins.add("http://127.0.0.1:8080");
            origins.add("http://127.0.0.1:8081");
            origins.add("http://127.0.0.1:8082");
            origins.add("http://127.0.0.1:8083");
            origins.add("http://127.0.0.1:8084");
            configuration.setAllowedOrigins(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
