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
@CrossOrigin(origins = "*") // Permite que Flutter se conecte sin bloqueos de CORS
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
    public ResponseEntity<Medication> crear(@RequestBody Medication medication) {
        return new ResponseEntity<>(medicationService.save(medication), HttpStatus.CREATED);
    }

    /**
     * Alternar estado de "tomado" (Lógica espejo de toggleMedicationTaken).
     * PATCH http://localhost:8082/api/medications/1/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Medication> toggleEstado(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(medicationService.toggleTaken(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Eliminar un medicamento.
     * DELETE http://localhost:8082/api/medications/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        medicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}