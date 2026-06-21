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
public class AutenticacionController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody User user) {
        try {
            User nuevoUsuario = userService.registerUser(user);
            return new ResponseEntity<>(Map.of(
                    "message", "Cuenta creada. Te enviamos un código de verificación a tu correo.",
                    "email", nuevoUsuario.getEmail(),
                    "requiresVerification", true), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Faltan datos"));
        }
        boolean ok = userService.verifyCode(email, code);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Código incorrecto o vencido"));
        }
        return ResponseEntity.ok(Map.of("message", "Cuenta verificada correctamente"));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Falta el correo"));
        }
        boolean ok = userService.resendCode(email);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No se pudo reenviar (cuenta inexistente o ya verificada)"));
        }
        return ResponseEntity.ok(Map.of("message", "Te enviamos un nuevo código"));
    }

    /** Envía un código de seguridad al correo para una acción sensible. */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String accion = body.getOrDefault("action", "continuar");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Falta el correo"));
        }
        userService.sendActionCode(email, accion);
        // Respuesta genérica: no revelamos si el correo existe o no.
        return ResponseEntity.ok(Map.of(
                "message", "Si el correo está registrado, te enviamos un código."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("password");
        if (email == null || code == null || newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Datos inválidos (revisa el código y que la contraseña tenga al menos 4 caracteres)"));
        }
        boolean ok = userService.resetPassword(email, code, newPassword);
        if (!ok) {
            return new ResponseEntity<>(Map.of("message", "Código incorrecto o vencido"),
                    HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
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

    @PostMapping("/enable-caregiver")
    public ResponseEntity<?> enableCaregiver(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Falta el código"));
        }
        boolean ok = userService.enableCaregiver(userId, code);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Código incorrecto o vencido"));
        }
        return ResponseEntity.ok(Map.of("message", "Función de cuidador activada"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || code == null || newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Completa la contraseña actual, el código y una nueva contraseña (mín. 4 caracteres)"));
        }
        boolean ok = userService.changePassword(userId, oldPassword, code, newPassword);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "La contraseña actual o el código son incorrectos"));
        }
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String password = body.get("password");
        String code = body.get("code");
        if (password == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Debes confirmar tu contraseña y el código"));
        }
        boolean ok = userService.deleteAccount(userId, password, code);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Contraseña o código incorrectos"));
        }
        return ResponseEntity.ok(Map.of("message", "Cuenta eliminada"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        return userService.findByEmail(email)
                .map(user -> {
                    // Verificamos si la contraseña coincide con el hash de la BD
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", "Credenciales inválidas"));
                    }
                    // Bloquear el acceso si la cuenta aún no fue verificada
                    if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                    "message", "Debes verificar tu correo antes de iniciar sesión",
                                    "requiresVerification", true,
                                    "email", user.getEmail()));
                    }
                    String token = jwtUtils.generateToken(user.getEmail(), user.getId());

                    // Devolvemos el token y datos básicos del perfil
                    return ResponseEntity.ok(Map.of(
                        "token", token,
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail()
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "El usuario no existe")));
    }
}