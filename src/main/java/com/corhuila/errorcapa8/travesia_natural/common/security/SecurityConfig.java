package com.corhuila.errorcapa8.travesia_natural.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary: permits every request. spring-boot-starter-security is on the classpath
 * (inherited from the project skeleton) and would otherwise block everything behind
 * basic auth. This is deliberate technical debt until a dedicated JWT spec replaces it
 * (spec 001, explicitly out of scope for this cut) — not a final security decision.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return http.build();
    }
}
