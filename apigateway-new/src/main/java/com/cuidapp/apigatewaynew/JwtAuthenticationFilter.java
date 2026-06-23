package com.cuidapp.apigatewaynew;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${app.jwt.secret:default_jwt_secret_for_dev}")
    private String jwtSecret;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        log.debug("Gateway filter start - path={}", path);

        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Email");
                })
                .build();

        if (isPublicPath(path) || HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            log.debug("Public path detected, skipping auth: {}", path);
            return chain.filter(exchange.mutate().request(sanitizedRequest).build());
        }

        String authHeader = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Token ausente");
        }

        String token = authHeader.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object rawUserId = claims.get("userId");
            if (rawUserId == null) {
                log.warn("JWT parsed but no userId claim present");
                return unauthorized(exchange, "Token sin userId");
            }

            String userId = String.valueOf(rawUserId);
            String email = claims.getSubject() == null ? "" : claims.getSubject();

            log.info("JWT valid - subject={}, userId={}", email, userId);

            ServerHttpRequest requestWithUser = sanitizedRequest.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email)
                    .build();

            return chain.filter(exchange.mutate().request(requestWithUser).build());
        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return unauthorized(exchange, "Token invalido");
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/api/auth/login")
            || path.equals("/api/auth/register")
            || path.equals("/api/auth/reset-password")
            || path.equals("/api/auth/verify")
            || path.equals("/api/auth/resend-code")
            || path.equals("/api/auth/send-code")
            || path.equals("/api/auth/vincular/confirmar");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("X-Error", message);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
