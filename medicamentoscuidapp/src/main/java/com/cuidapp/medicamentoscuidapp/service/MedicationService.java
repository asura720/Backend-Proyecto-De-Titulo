package com.cuidapp.medicamentoscuidapp.service;

import com.cuidapp.medicamentoscuidapp.model.Medication;
import com.cuidapp.medicamentoscuidapp.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;

    // URL del servicio de notificaciones. Por defecto localhost (Laragon/local),
    // se sobrescribe con la variable de entorno NOTIFICATIONS_SERVICE_URL en Docker.
    @Value("${notifications.service.url:http://localhost:8084}")
    private String notificationsServiceUrl;

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
        if (medication.getIsTaken() == null) {
            medication.setTaken(false);
        }
        return medicationRepository.save(medication);
    }

    // After saving a medication, schedule notification by calling notifications service
    public Medication saveAndNotify(Medication medication) {
        Medication saved = save(medication);

        // Call notifications service (via gateway) asynchronously
        try {
            var rest = new org.springframework.web.client.RestTemplate();
            var req = new HashMap<String, Object>();
            req.put("userId", saved.getUserId());
            req.put("medicationId", saved.getId());
            req.put("medicationName", saved.getName());
            req.put("scheduledAt", java.time.LocalDateTime.now().plusHours(1).toString());
            req.put("title", "Hora de tu medicamento");
            req.put("message", "Es hora de tomar " + saved.getName());

            String notificationsService = notificationsServiceUrl + "/api/notifications/schedule";
            rest.postForEntity(notificationsService, req, String.class);
        } catch (Exception e) {
            // log but do not fail the medication save
            System.err.println("Failed to call notifications service: " + e.getMessage());
        }

        return saved;
    }

    public Medication update(Long id, Medication data, Long userId) {
        return medicationRepository.findById(id).map(med -> {
            if (!med.getUserId().equals(userId)) {
                throw new RuntimeException("No autorizado");
            }
            med.setName(data.getName());
            med.setDosage(data.getDosage());
            med.setFrequency(data.getFrequency());
            med.setTimes(data.getTimes());
            med.setContainerColor(data.getContainerColor());
            med.setIconColor(data.getIconColor());
            return medicationRepository.save(med);
        }).orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));
    }

    public Medication toggleTaken(Long id, Long userId) {
        return medicationRepository.findById(id).map(med -> {
            if (!med.getUserId().equals(userId)) {
                throw new RuntimeException("No autorizado");
            }
            boolean newState = !med.isTaken();
            med.setTaken(newState);
            med.setTakenDateTime(newState ? LocalDateTime.now() : null);
            return medicationRepository.save(med);
        }).orElseThrow(() -> new RuntimeException("No se encontró el medicamento con ID: " + id));
    }

    public void delete(Long id, Long userId) {
        medicationRepository.findById(id).ifPresentOrElse(med -> {
            if (!med.getUserId().equals(userId)) {
                throw new RuntimeException("No autorizado");
            }
            medicationRepository.deleteById(id);
        }, () -> { throw new RuntimeException("Medicamento no encontrado"); });
    }

    /**
     * Buscar por ID individual.
     */
    public Optional<Medication> getById(Long id) {
        return medicationRepository.findById(id);
    }
}