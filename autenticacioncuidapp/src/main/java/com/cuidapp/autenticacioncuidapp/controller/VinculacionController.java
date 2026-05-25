package com.cuidapp.autenticacioncuidapp.controller;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.service.VinculacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/vincular")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VinculacionController {

    private final VinculacionService vinculacionService;

    // El titular invita a un paciente por email
    // POST /api/auth/vincular/invitar
    // Header: X-User-Id (lo pone el gateway)
    // Body: { "emailPaciente": "abuela@gmail.com" }
    @PostMapping("/invitar")
    public ResponseEntity<?> invitar(
            @RequestHeader("X-User-Id") Long titularId,
            @RequestBody Map<String, String> body) {
        try {
            vinculacionService.invitar(titularId, body.get("emailPaciente"));
            return ResponseEntity.ok(Map.of("message", "Invitación enviada al correo del paciente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // El paciente acepta desde el link del email (sin JWT — acceso público)
    // GET /api/auth/vincular/aceptar/{token}
    @GetMapping("/aceptar/{token}")
    public ResponseEntity<String> aceptar(@PathVariable String token) {
        try {
            String resultado = vinculacionService.aceptar(token);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // El paciente rechaza desde el link del email (sin JWT — acceso público)
    // GET /api/auth/vincular/rechazar/{token}
    @GetMapping("/rechazar/{token}")
    public ResponseEntity<String> rechazar(@PathVariable String token) {
        try {
            String resultado = vinculacionService.rechazar(token);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // El titular obtiene su lista de pacientes
    // GET /api/auth/vincular/mis-pacientes
    @GetMapping("/mis-pacientes")
    public ResponseEntity<List<User>> getMisPacientes(@RequestHeader("X-User-Id") Long titularId) {
        return ResponseEntity.ok(vinculacionService.getMisPacientes(titularId));
    }

    // El paciente obtiene quién es su titular
    // GET /api/auth/vincular/mi-titular
    @GetMapping("/mi-titular")
    public ResponseEntity<?> getMiTitular(@RequestHeader("X-User-Id") Long pacienteId) {
        try {
            return ResponseEntity.ok(vinculacionService.getMiTitular(pacienteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // El titular desvincula a un paciente
    // DELETE /api/auth/vincular/desvincular/{pacienteId}
    @DeleteMapping("/desvincular/{pacienteId}")
    public ResponseEntity<?> desvincular(
            @RequestHeader("X-User-Id") Long titularId,
            @PathVariable Long pacienteId) {
        try {
            vinculacionService.desvincular(titularId, pacienteId);
            return ResponseEntity.ok(Map.of("message", "Vinculación eliminada"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
