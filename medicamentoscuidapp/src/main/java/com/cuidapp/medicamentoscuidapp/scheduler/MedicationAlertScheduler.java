package com.cuidapp.medicamentoscuidapp.scheduler;

import com.cuidapp.medicamentoscuidapp.model.Medication;
import com.cuidapp.medicamentoscuidapp.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Revisa periódicamente los medicamentos. Si una dosis programada lleva más de
 * 15 minutos de atraso y no fue tomada ese día, envía una alerta (push + vibración)
 * al cuidador del paciente (o al mismo paciente si es independiente).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationAlertScheduler {

    private final MedicationRepository medicationRepository;

    @Value("${notifications.service.url:http://localhost:8084}")
    private String notificationsServiceUrl;

    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${med.alert.grace-minutes:15}")
    private int graceMinutes;

    private final RestTemplate rest = new RestTemplate();

    // Evita alertar repetido para la misma dosis (medId|hora|fecha)
    private final Set<String> alerted = ConcurrentHashMap.newKeySet();

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})");

    @Scheduled(fixedDelayString = "${med.alert.delay:60000}")
    public void checkOverdue() {
        try {
            final LocalDate today = LocalDate.now();
            final LocalDateTime now = LocalDateTime.now();

            for (Medication m : medicationRepository.findAll()) {
                if (m.getTimes() == null) continue;

                // Si tiene días definidos y hoy no es uno de ellos, no se alerta
                if (!scheduledToday(m, today)) continue;

                for (String timeStr : m.getTimes()) {
                    LocalTime dose = parseTime(timeStr);
                    if (dose == null) continue;

                    LocalDateTime due = LocalDateTime.of(today, dose);
                    LocalDateTime threshold = due.plusMinutes(graceMinutes);

                    // Avisar solo si ya pasó el margen y no hace demasiado (ventana de 6h)
                    if (now.isAfter(threshold) && now.isBefore(due.plusHours(6))) {
                        if (doseTaken(m, today, timeStr)) continue;

                        String key = m.getId() + "|" + timeStr + "|" + today;
                        if (!alerted.add(key)) continue; // ya se alertó esta dosis hoy

                        sendAlert(m);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[MedAlert] Error revisando dosis: {}", e.getMessage());
        }
    }

    /** True si el medicamento corresponde tomarse hoy (sin días = todos los días). */
    private boolean scheduledToday(Medication m, LocalDate today) {
        if (m.getDays() == null || m.getDays().isEmpty()) return true;
        String dow = String.valueOf(today.getDayOfWeek().getValue()); // 1=Lunes ... 7=Domingo
        return m.getDays().contains(dow);
    }

    /** True si esa dosis puntual (hoy + horario) ya fue marcada como tomada. */
    private boolean doseTaken(Medication m, LocalDate today, String timeStr) {
        return m.getTakenDoses() != null
                && m.getTakenDoses().contains(today + "|" + timeStr);
    }

    /** Parser tolerante: "08:00", "8:00", "8:00 PM", "08:00 p. m." */
    private LocalTime parseTime(String raw) {
        if (raw == null) return null;
        Matcher matcher = TIME_PATTERN.matcher(raw);
        if (!matcher.find()) return null;
        try {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            String lower = raw.toLowerCase();
            boolean pm = lower.contains("p");
            boolean am = lower.contains("a") && !lower.contains("p");
            if (pm && hour < 12) hour += 12;
            if (am && hour == 12) hour = 0;
            if (hour > 23 || minute > 59) return null;
            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendAlert(Medication m) {
        try {
            // 1. ¿A quién avisar? La alerta llega SOLO a las personas del vínculo:
            //    el propio paciente y, si está vinculado, también su cuidador.
            String targetUrl = authServiceUrl + "/api/auth/vincular/alert-target/" + m.getUserId();
            @SuppressWarnings("unchecked")
            Map<String, Object> target = rest.getForObject(targetUrl, Map.class);
            if (target == null || target.get("recipients") == null) {
                log.warn("[MedAlert] No se pudo resolver destinatarios para userId={}", m.getUserId());
                return;
            }

            String patientName = (String) target.get("patientName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recipients =
                    (List<Map<String, Object>>) target.get("recipients");

            // 2. Enviar la alerta a cada destinatario del vínculo
            for (Map<String, Object> r : recipients) {
                if (r.get("userId") == null) continue;
                Long targetUserId = ((Number) r.get("userId")).longValue();
                boolean self = Boolean.TRUE.equals(r.get("self"));

                Map<String, Object> body = new HashMap<>();
                body.put("targetUserId", targetUserId);
                body.put("patientName", self ? null : patientName);
                body.put("medicationName", m.getName());

                rest.postForEntity(notificationsServiceUrl + "/api/notifications/medication-alert",
                        body, String.class);

                log.info("[MedAlert] Alerta enviada: medId={}, '{}', targetUserId={}, self={}",
                        m.getId(), m.getName(), targetUserId, self);
            }
        } catch (Exception e) {
            log.error("[MedAlert] No se pudo enviar alerta para medId={}: {}", m.getId(), e.getMessage());
        }
    }
}
