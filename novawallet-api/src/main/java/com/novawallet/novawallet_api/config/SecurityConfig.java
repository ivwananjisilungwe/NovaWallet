package com.novawallet.novawallet_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                // REST API does not use browser sessions
                .csrf(csrf -> csrf.disable())


                // Security headers
                .headers(headers -> headers

                        // Prevent clickjacking attacks
                        .frameOptions(frame -> frame.sameOrigin())

                        // Prevent MIME sniffing
                        .contentTypeOptions(contentType -> {})

                        // Force HTTPS in production
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                )


                // Phase 0:
                // Everything public
                // JWT will replace this later
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/actuator/**"
                        ).permitAll()

                        .anyRequest().permitAll()
                )


                // Disable default login page
                .formLogin(form -> form.disable())

                // Disable browser basic authentication popup
                .httpBasic(basic -> basic.disable());


        return http.build();
    }

    // Phase 0 placeholder
    // Real database user authentication comes later
    @Bean
    public UserDetailsService userDetailsService() {

        return username -> {
            throw new UsernameNotFoundException(
                    "Users are not implemented yet"
            );
        };
    }

}