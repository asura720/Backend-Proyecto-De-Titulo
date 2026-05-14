package com.cuidapp.autenticacioncuidapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // <--- ¡VITAL! Le dice a Spring que aquí hay configuraciones
@EnableWebSecurity // <--- Activa la seguridad web de Spring
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Agregamos esto para que no te pida contraseña al entrar desde el navegador/Postman por ahora
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para pruebas de API
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Permitir todas las rutas por ahora
            );
        return http.build();
    }
}