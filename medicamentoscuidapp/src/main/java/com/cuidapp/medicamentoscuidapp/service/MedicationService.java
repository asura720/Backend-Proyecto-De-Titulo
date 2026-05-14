package com.cuidapp.medicamentoscuidapp.service;

import com.cuidapp.medicamentoscuidapp.model.Medication;
import com.cuidapp.medicamentoscuidapp.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;

    /**
     * Obtiene todos los medicamentos de un usuario.
     * Útil para que el Provider de Flutter cargue la lista inicial.
     */
    public List<Medication> getByUserId(Long userId) {
        return medicationRepository.findByUserId(userId);
    }

    /**
     * Guarda un nuevo medicamento o actualiza uno existente.
     */
    public Medication save(Medication medication) {
        return medicationRepository.save(medication);
    }

    /**
     * Lógica espejo de toggleMedicationTaken de tu Flutter Provider.
     * Si se marca como tomado, registra la fecha/hora actual de Laragon.
     */
    public Medication toggleTaken(Long id) {
        return medicationRepository.findById(id).map(med -> {
            // Cambiamos el estado (true/false)
            boolean newState = !med.isTaken();
            med.setTaken(newState);
            
            // Si es true, ponemos la hora actual. Si es false, la borramos.
            if (newState) {
                med.setTakenDateTime(LocalDateTime.now());
            } else {
                med.setTakenDateTime(null);
            }
            
            return medicationRepository.save(med);
        }).orElseThrow(() -> new RuntimeException("No se encontró el medicamento con ID: " + id));
    }

    /**
     * Elimina el medicamento de la base de datos.
     */
    public void delete(Long id) {
        medicationRepository.deleteById(id);
    }

    /**
     * Buscar por ID individual.
     */
    public Optional<Medication> getById(Long id) {
        return medicationRepository.findById(id);
    }
}