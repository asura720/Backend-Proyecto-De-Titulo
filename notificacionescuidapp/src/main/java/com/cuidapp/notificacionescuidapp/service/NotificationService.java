package com.cuidapp.notificacionescuidapp.service;

import com.cuidapp.notificacionescuidapp.dto.NotificationRequest;
import com.cuidapp.notificacionescuidapp.model.Notification;
import com.cuidapp.notificacionescuidapp.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    /** Elimina todas las notificaciones programadas de un medicamento. */
    @Transactional
    public long deleteByMedication(Long medicationId) {
        return repository.deleteByMedicationId(medicationId);
    }

    public Notification saveFromRequest(NotificationRequest req) {
        LocalDateTime scheduled;
        try {
            scheduled = LocalDateTime.parse(req.getScheduledAt());
        } catch (DateTimeParseException e) {
            scheduled = LocalDateTime.now();
        }

        Notification n = Notification.builder()
                .userId(req.getUserId())
                .medicationId(req.getMedicationId())
                .medicationName(req.getMedicationName())
                .title(req.getTitle())
                .message(req.getMessage())
                .scheduledAt(scheduled)
                .build();

        return repository.save(n);
    }
}
