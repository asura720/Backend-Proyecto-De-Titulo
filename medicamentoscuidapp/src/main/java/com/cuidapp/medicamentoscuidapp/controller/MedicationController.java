package com.cuidapp.medicamentoscuidapp.controller;

import com.cuidapp.medicamentoscuidapp.model.Medication;
import com.cuidapp.medicamentoscuidapp.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    /**
     * Obtener todos los medicamentos de un usuario específico.
     * GET http://localhost:8082/api/medications/user/1
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Medication>> obtenerPorUsuario(@PathVariable Long userId) {
        return ResponseEntity.ok(medicationService.getByUserId(userId));
    }

    /**
     * Crear un nuevo medicamento (Desde el formulario o tras el OCR).
     * POST http://localhost:8082/api/medications
     */
    @PostMapping
    public ResponseEntity<Medication> crear(
            @RequestBody Medication medication,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Si el body no trae un paciente destino, se asigna al usuario autenticado.
        // El titular envía userId para registrar medicamentos a su paciente.
        if (medication.getUserId() == null) {
            medication.setUserId(userId);
        }
        return new ResponseEntity<>(medicationService.saveAndNotify(medication), HttpStatus.CREATED);
    }

    /**
     * Editar un medicamento existente.
     * PUT http://localhost:8082/api/medications/1
     */
    @PutMapping("/{id}")
    public ResponseEntity<Medication> editar(
            @PathVariable Long id,
            @RequestBody Medication data,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(medicationService.update(id, data, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Alternar estado de "tomado" (Lógica espejo de toggleMedicationTaken).
     * PATCH http://localhost:8082/api/medications/1/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Medication> toggleEstado(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(medicationService.toggleTaken(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Marcar/desmarcar una dosis puntual de hoy (por horario).
     * PATCH http://localhost:8082/api/medications/1/dose
     * Body: { "time": "08:00", "taken": true }
     */
    @PatchMapping("/{id}/dose")
    public ResponseEntity<Medication> marcarDosis(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String time = body.get("time") != null ? body.get("time").toString() : null;
        boolean taken = !Boolean.FALSE.equals(body.get("taken"));
        if (time == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(medicationService.markDose(id, time, taken, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Eliminar un medicamento.
     * DELETE http://localhost:8082/api/medications/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            medicationService.delete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}