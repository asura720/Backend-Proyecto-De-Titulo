package com.cuidapp.notificacionescuidapp.service;

import com.cuidapp.notificacionescuidapp.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationSender {

    public boolean send(Notification n) {
        log.info("Sending notification: userId={}, medId={}, scheduledAt={}, title={} - {}",
                n.getUserId(), n.getMedicationId(), n.getScheduledAt(), n.getTitle(), n.getMessage());
        return true;
    }
}
