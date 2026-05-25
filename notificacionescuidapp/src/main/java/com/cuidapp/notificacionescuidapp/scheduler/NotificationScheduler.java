package com.cuidapp.notificacionescuidapp.scheduler;

import com.cuidapp.notificacionescuidapp.model.Notification;
import com.cuidapp.notificacionescuidapp.repository.NotificationRepository;
import com.cuidapp.notificacionescuidapp.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationRepository repository;
    private final NotificationSender sender;

    @Scheduled(fixedDelayString = "${notifications.scheduler.delay:60000}")
    public void run() {
        try {
            List<Notification> due = repository.findBySentFalseAndScheduledAtBefore(LocalDateTime.now());
            for (Notification n : due) {
                boolean ok = sender.send(n);
                if (ok) {
                    n.setSent(true);
                    repository.save(n);
                    log.info("Notification sent and marked: id={}", n.getId());
                } else {
                    log.warn("Failed to send notification id={}", n.getId());
                }
            }
        } catch (Exception e) {
            log.error("Scheduler error", e);
        }
    }
}
