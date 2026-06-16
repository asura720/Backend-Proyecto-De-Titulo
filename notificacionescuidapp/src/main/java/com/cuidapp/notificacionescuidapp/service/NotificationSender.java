package com.cuidapp.notificacionescuidapp.service;

import com.cuidapp.notificacionescuidapp.model.DeviceToken;
import com.cuidapp.notificacionescuidapp.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSender {

    private final DeviceTokenService deviceTokenService;
    private final FcmService fcmService;

    public boolean send(Notification n) {
        List<DeviceToken> tokens = deviceTokenService.forUser(n.getUserId());
        if (tokens.isEmpty()) {
            log.warn("No hay tokens de dispositivo para userId={}; notificación id={} no se envía por push",
                    n.getUserId(), n.getId());
            // Devolvemos true para marcarla como procesada y no reintentar en bucle.
            return true;
        }

        String title = n.getTitle() != null ? n.getTitle() : "Recordatorio de CuidApp";
        String body = n.getMessage() != null ? n.getMessage() : "Tienes una notificación pendiente";

        for (DeviceToken t : tokens) {
            fcmService.sendToToken(t.getToken(), title, body);
        }
        return true;
    }
}
