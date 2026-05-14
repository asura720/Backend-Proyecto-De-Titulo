package com.cuidapp.autenticacioncuidapp.controller;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite que tu app Flutter se conecte sin bloqueos de CORS
public class AutenticacionController {

    private final UserService userService;

    /**
     * Endpoint para registrar un nuevo usuario.
     * URL: POST http://localhost:8081/api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody User user) {
        try {
            User nuevoUsuario = userService.registerUser(user);
            return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint para el inicio de sesión.
     * URL: POST http://localhost:8081/api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        return userService.findByEmail(email)
                .map(user -> {
                    // Por ahora validamos si el usuario existe. 
                    // En la siguiente fase implementaremos la validación de contraseña con JWT.
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}