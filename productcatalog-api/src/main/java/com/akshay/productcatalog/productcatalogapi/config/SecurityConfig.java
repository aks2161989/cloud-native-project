package com.akshay.productcatalog.productcatalogapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
            http.csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authorizeHttpRequests(auth -> auth
                            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                            .requestMatchers("/healthz").permitAll()
                            .requestMatchers("/validateEmail").permitAll()
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/v1/user"
                            ).permitAll()
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/v1/product/**"
                            ).permitAll()
                            .anyRequest().authenticated()
                    )
                    .httpBasic(Customizer.withDefaults());

            return http.build();
    }
}
