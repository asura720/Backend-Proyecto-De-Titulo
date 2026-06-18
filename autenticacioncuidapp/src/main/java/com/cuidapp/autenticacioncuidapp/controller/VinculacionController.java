package com.cuidapp.autenticacioncuidapp.controller;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.service.VinculacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/vincular")
@RequiredArgsConstructor
public class VinculacionController {

    private final VinculacionService vinculacionService;

    // El titular crea la cuenta de un paciente bajo su cargo
    // POST /api/auth/vincular/crear-paciente
    // Header: X-User-Id (lo pone el gateway)
    // Body: { "name", "email", "password", "birthDate"?, "bloodType"? }
    @PostMapping("/crear-paciente")
    public ResponseEntity<?> crearPaciente(
            @RequestHeader("X-User-Id") Long titularId,
            @RequestBody Map<String, String> body) {
        try {
            String birthDateStr = body.get("birthDate");
            LocalDate birthDate = (birthDateStr != null && !birthDateStr.isEmpty())
                    ? LocalDate.parse(birthDateStr) : null;

            User paciente = vinculacionService.crearPaciente(
                    titularId,
                    body.get("name"),
                    body.get("email"),
                    body.get("password"),
                    birthDate,
                    body.get("bloodType")
            );
            return ResponseEntity.ok(paciente);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // El titular obtiene su lista de pacientes (con estado de permiso/solicitud)
    // GET /api/auth/vincular/mis-pacientes
    @GetMapping("/mis-pacientes")
    public ResponseEntity<List<Map<String, Object>>> getMisPacientes(@RequestHeader("X-User-Id") Long titularId) {
        return ResponseEntity.ok(vinculacionService.getMisPacientes(titularId));
    }

    // El paciente solicita permiso para gestionar sus propios medicamentos
    // POST /api/auth/vincular/solicitar-permiso
    @PostMapping("/solicitar-permiso")
    public ResponseEntity<?> solicitarPermiso(@RequestHeader("X-User-Id") Long pacienteId) {
        try {
            vinculacionService.solicitarPermiso(pacienteId);
            return ResponseEntity.ok(Map.of("message", "Solicitud enviada a tu cuidador"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // El titular responde la solicitud de un paciente
    // POST /api/auth/vincular/responder-solicitud/{pacienteId}  Body: { "aprobar": true }
    @PostMapping("/responder-solicitud/{pacienteId}")
    public ResponseEntity<?> responderSolicitud(
            @RequestHeader("X-User-Id") Long titularId,
            @PathVariable Long pacienteId,
            @RequestBody Map<String, Boolean> body) {
        try {
            vinculacionService.responderSolicitud(titularId, pacienteId, Boolean.TRUE.equals(body.get("aprobar")));
            return ResponseEntity.ok(Map.of("message", "Solicitud respondida"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // El paciente consulta su estado de permiso
    // GET /api/auth/vincular/mi-permiso
    @GetMapping("/mi-permiso")
    public ResponseEntity<?> getMiPermiso(@RequestHeader("X-User-Id") Long pacienteId) {
        try {
            return ResponseEntity.ok(vinculacionService.getMiPermiso(pacienteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Uso interno (servicio de medicamentos): a quién avisar si un paciente no toma su medicamento
    // GET /api/auth/vincular/alert-target/{pacienteId}
    @GetMapping("/alert-target/{pacienteId}")
    public ResponseEntity<?> alertTarget(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(vinculacionService.resolveAlertTarget(pacienteId));
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
