package com.cuidapp.notificacionescuidapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Inicializa Firebase Admin SDK al arrancar, solo si app.fcm.enabled=true y existe
 * el archivo de credenciales (cuenta de servicio). Si no, la app arranca igual y el
 * envío de push queda en modo simulado (ver FcmService).
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Value("${app.fcm.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (!fcmEnabled) {
            log.info("[Firebase] FCM deshabilitado (app.fcm.enabled=false). Push en modo simulado.");
            return;
        }
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("[Firebase] Ya estaba inicializado.");
                return;
            }
            try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("[Firebase] Inicializado correctamente desde {}", credentialsPath);
            }
        } catch (Exception e) {
            log.error("[Firebase] No se pudo inicializar ({}): {}. El push quedará en modo simulado.",
                    credentialsPath, e.getMessage());
        }
    }
}
