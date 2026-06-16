package com.cuidapp.notificacionescuidapp.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Envía notificaciones push a través de Firebase Cloud Messaging.
 *
 * Es tolerante: si FCM está deshabilitado o Firebase no se inicializó (sin
 * credenciales), no falla; solo escribe un log (modo simulado).
 */
@Service
@Slf4j
public class FcmService {

    @Value("${app.fcm.enabled:false}")
    private boolean fcmEnabled;

    public void sendToToken(String token, String title, String body) {
        if (!fcmEnabled || FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM] (simulado) push -> token={}..., título='{}', mensaje='{}'",
                    abbreviate(token), title, body);
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Push enviado a token={}...: {}", abbreviate(token), response);
        } catch (Exception e) {
            log.error("[FCM] Error enviando push a token={}...: {}", abbreviate(token), e.getMessage());
        }
    }

    /**
     * Envía una alerta de alta prioridad (p. ej. SOS) con datos extra y un canal
     * específico en Android, para que el dispositivo vibre y suene aunque la app
     * esté en segundo plano.
     */
    public void sendAlert(String token, String title, String body,
                          Map<String, String> data, String channelId) {
        if (!fcmEnabled || FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM] (simulado) ALERTA -> token={}..., canal={}, título='{}', mensaje='{}', data={}",
                    abbreviate(token), channelId, title, body, data);
            return;
        }
        try {
            AndroidConfig android = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setChannelId(channelId)
                            .setPriority(AndroidNotification.Priority.MAX)
                            .build())
                    .build();

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(android)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Alerta enviada a token={}...: {}", abbreviate(token), response);
        } catch (Exception e) {
            log.error("[FCM] Error enviando alerta a token={}...: {}", abbreviate(token), e.getMessage());
        }
    }

    private String abbreviate(String token) {
        if (token == null) return "null";
        return token.length() <= 12 ? token : token.substring(0, 12);
    }
}
