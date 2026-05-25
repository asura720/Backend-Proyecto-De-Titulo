package com.cuidapp.autenticacioncuidapp.controller;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.service.UserService;
import com.cuidapp.autenticacioncuidapp.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class AutenticacionController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody User user) {
        try {
            User nuevoUsuario = userService.registerUser(user);
            return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return userService.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody User data) {
        try {
            return ResponseEntity.ok(userService.updateProfile(userId, data));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        return userService.findByEmail(email)
                .map(user -> {
                    // Verificamos si la contraseña coincide con el hash de la BD
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        String token = jwtUtils.generateToken(user.getEmail(), user.getId());
                        
                        // Devolvemos el token y datos básicos del perfil
                        return ResponseEntity.ok(Map.of(
                            "token", token,
                            "id", user.getId(),
                            "name", user.getName(),
                            "email", user.getEmail()
                        ));
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", "Credenciales inválidas"));
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "El usuario no existe")));
    }
}