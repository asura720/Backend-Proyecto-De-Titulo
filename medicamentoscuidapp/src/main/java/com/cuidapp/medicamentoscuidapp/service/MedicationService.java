package com.cuidapp.medicamentoscuidapp.service;

import com.cuidapp.medicamentoscuidapp.model.Medication;
import com.cuidapp.medicamentoscuidapp.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;

    // URL del servicio de notificaciones. Por defecto localhost (Laragon/local),
    // se sobrescribe con la variable de entorno NOTIFICATIONS_SERVICE_URL en Docker.
    @Value("${notifications.service.url:http://localhost:8084}")
    private String notificationsServiceUrl;

    // URL del servicio de autenticación, para resolver los destinatarios del vínculo.
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;

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

    // Tras guardar un medicamento, programa el recordatorio. La notificación llega
    // SOLO a las personas del vínculo: el paciente y, si está vinculado, su cuidador.
    public Medication saveAndNotify(Medication medication) {
        Medication saved = save(medication);

        try {
            var rest = new org.springframework.web.client.RestTemplate();
            String scheduledAt = java.time.LocalDateTime.now().plusHours(1).toString();
            String notificationsService = notificationsServiceUrl + "/api/notifications/schedule";

            // Resolver destinatarios del vínculo (paciente + cuidador si existe)
            for (Map<String, Object> r : resolveRecipients(rest, saved.getUserId())) {
                Long targetUserId = ((Number) r.get("userId")).longValue();
                boolean self = Boolean.TRUE.equals(r.get("self"));
                String patientName = (String) r.get("patientName");

                var req = new HashMap<String, Object>();
                req.put("userId", targetUserId);
                req.put("medicationId", saved.getId());
                req.put("medicationName", saved.getName());
                req.put("scheduledAt", scheduledAt);
                req.put("title", "Hora del medicamento");
                req.put("message", self
                        ? "Es hora de tomar " + saved.getName()
                        : (patientName != null && !patientName.isBlank()
                            ? "Es hora de que " + patientName + " tome " + saved.getName()
                            : "Es hora del medicamento " + saved.getName()));

                rest.postForEntity(notificationsService, req, String.class);
            }
        } catch (Exception e) {
            // log but do not fail the medication save
            System.err.println("Failed to call notifications service: " + e.getMessage());
        }

        return saved;
    }

    /**
     * Pregunta al servicio de autenticación quiénes son los destinatarios del
     * vínculo (paciente + cuidador). Si falla, devuelve solo al propio usuario.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveRecipients(
            org.springframework.web.client.RestTemplate rest, Long pacienteId) {
        try {
            String url = authServiceUrl + "/api/auth/vincular/alert-target/" + pacienteId;
            Map<String, Object> target = rest.getForObject(url, Map.class);
            if (target != null && target.get("recipients") != null) {
                String patientName = (String) target.get("patientName");
                List<Map<String, Object>> recipients =
                        (List<Map<String, Object>>) target.get("recipients");
                // Adjuntar el nombre del paciente a cada destinatario para el mensaje
                List<Map<String, Object>> out = new java.util.ArrayList<>();
                for (Map<String, Object> r : recipients) {
                    Map<String, Object> m = new HashMap<>(r);
                    m.put("patientName", patientName);
                    out.add(m);
                }
                return out;
            }
        } catch (Exception ignored) {
            // Sin auth disponible: caer al propio usuario
        }
        return List.of(Map.of("userId", pacienteId, "self", true));
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
            med.setDays(data.getDays());
            med.setContainerColor(data.getContainerColor());
            med.setIconColor(data.getIconColor());
            return medicationRepository.save(med);
        }).orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));
    }

    /**
     * Marca (o desmarca) una dosis puntual de hoy, identificada por su horario "HH:mm".
     * Guarda la entrada "YYYY-MM-DD|HH:mm" en takenDoses.
     */
    public Medication markDose(Long id, String time, boolean taken, Long userId) {
        return medicationRepository.findById(id).map(med -> {
            if (!med.getUserId().equals(userId)) {
                throw new RuntimeException("No autorizado");
            }
            final String today = java.time.LocalDate.now().toString();
            final String key = today + "|" + time;
            final List<String> doses = med.getTakenDoses() != null
                    ? med.getTakenDoses()
                    : new java.util.ArrayList<>();
            if (taken) {
                if (!doses.contains(key)) doses.add(key);
            } else {
                doses.remove(key);
            }
            med.setTakenDoses(doses);

            // Compatibilidad: isTaken/takenDateTime reflejan si hoy quedó completo
            boolean allTakenToday = med.getTimes() != null && !med.getTimes().isEmpty()
                    && med.getTimes().stream()
                        .allMatch(t -> doses.contains(today + "|" + t));
            med.setTaken(allTakenToday);
            med.setTakenDateTime(taken ? LocalDateTime.now()
                    : (allTakenToday ? med.getTakenDateTime() : null));

            return medicationRepository.save(med);
        }).orElseThrow(() -> new RuntimeException("No se encontró el medicamento con ID: " + id));
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
            // Eliminar también las notificaciones programadas de este medicamento
            try {
                new org.springframework.web.client.RestTemplate().delete(
                        notificationsServiceUrl + "/api/notifications/medication/" + id);
            } catch (Exception e) {
                System.err.println("No se pudieron borrar notificaciones del medicamento "
                        + id + ": " + e.getMessage());
            }
        }, () -> { throw new RuntimeException("Medicamento no encontrado"); });
    }

    /**
     * Buscar por ID individual.
     */
    public Optional<Medication> getById(Long id) {
        return medicationRepository.findById(id);
    }
}