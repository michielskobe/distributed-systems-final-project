package com.frontend.dsgt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Value("${okta.oauth2.issuer}")
    private String issuer;
    @Value("${okta.oauth2.client-id}")
    private String clientId;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/admin/**").hasAuthority("Manager")
                    .requestMatchers("/favicon.ico").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                    .requestMatchers("/", "/products").permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
            .accessDeniedPage("/error/access-denied")
            )
                .oauth2Login(withDefaults())
            //.oauth2ResourceServer(jwt -> jwt.jwt(withDefaults()))
            .logout(logout -> logout
                    .addLogoutHandler(logoutHandler()));
        return http.build();
    }

    private LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            try {
                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                response.sendRedirect(issuer + "v2/logout?client_id=" + clientId + "&returnTo=" + baseUrl);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
