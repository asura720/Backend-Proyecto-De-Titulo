package com.cuidapp.autenticacioncuidapp.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    // IMPORTANTE: En producción, esta clave debe venir de una variable de entorno.
    // Debe tener al menos 32 caracteres para cumplir con el algoritmo HS256.
    private final String JWT_SECRET = "esta_es_una_clave_muy_secreta_y_larga_para_cuidapp_2026";
    
    // El token expirará en 24 horas (en milisegundos)
    private final long JWT_EXPIRATION = 86400000L;

    /**
     * Genera un token basado en el email del usuario.
     */
    public String generateToken(String email) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(email) // El "dueño" del token
                .issuedAt(new Date()) // Fecha de creación
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION)) // Fecha de vencimiento
                .signWith(key) // Firma digital
                .compact();
    }

    /**
     * Valida si un token es estructuralmente correcto y no ha expirado.
     * (Esto lo usaremos más adelante en el API Gateway)
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}