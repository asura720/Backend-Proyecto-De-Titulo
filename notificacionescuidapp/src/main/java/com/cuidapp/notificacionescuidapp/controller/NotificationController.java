package com.cuidapp.notificacionescuidapp.controller;

import com.cuidapp.notificacionescuidapp.dto.DeviceTokenRequest;
import com.cuidapp.notificacionescuidapp.dto.NotificationRequest;
import com.cuidapp.notificacionescuidapp.dto.SosRequest;
import com.cuidapp.notificacionescuidapp.dto.WelcomeRequest;
import com.cuidapp.notificacionescuidapp.model.DeviceToken;
import com.cuidapp.notificacionescuidapp.model.Notification;
import com.cuidapp.notificacionescuidapp.service.DeviceTokenService;
import com.cuidapp.notificacionescuidapp.service.FcmService;
import com.cuidapp.notificacionescuidapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final DeviceTokenService deviceTokenService;
    private final FcmService fcmService;

    @PostMapping("/schedule")
    public ResponseEntity<Notification> schedule(@Validated @RequestBody NotificationRequest req) {
        Notification saved = service.saveFromRequest(req);
        return ResponseEntity.ok(saved);
    }

    /**
     * Registra el token FCM del dispositivo para un usuario.
     * Lo llama la app tras iniciar sesión.
     */
    @PostMapping("/token")
    public ResponseEntity<?> registerToken(@Validated @RequestBody DeviceTokenRequest req) {
        deviceTokenService.register(req.getUserId(), req.getToken());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Envía un push de bienvenida inmediato a todos los dispositivos del usuario.
     * Lo llama la app justo después del registro (cuando el token ya fue registrado).
     */
    @PostMapping("/welcome")
    public ResponseEntity<?> welcome(@Validated @RequestBody WelcomeRequest req) {
        List<DeviceToken> tokens = deviceTokenService.forUser(req.getUserId());
        String nombre = (req.getName() != null && !req.getName().isBlank()) ? req.getName() : "";
        String body = nombre.isEmpty()
                ? "Tu cuenta fue creada correctamente. Gestiona tus medicamentos y recordatorios desde la app."
                : "¡Hola " + nombre + "! Tu cuenta fue creada correctamente. Bienvenido a CuidApp.";
        for (DeviceToken t : tokens) {
            fcmService.sendToToken(t.getToken(), "¡Bienvenido a CuidApp! 💙", body);
        }
        return ResponseEntity.ok(Map.of("sent", tokens.size()));
    }

    /**
     * Botón de emergencia (SOS): el paciente lo pulsa y se envía una alerta de
     * alta prioridad (con vibración) a todos los dispositivos de su cuidador.
     */
    @PostMapping("/sos")
    public ResponseEntity<?> sos(@Validated @RequestBody SosRequest req) {
        List<DeviceToken> tokens = deviceTokenService.forUser(req.getCaregiverId());
        String patient = (req.getPatientName() != null && !req.getPatientName().isBlank())
                ? req.getPatientName()
                : "Tu paciente";
        String title = "🆘 ¡SOS de " + patient + "!";
        String body = patient + " ha pulsado el botón de emergencia y necesita ayuda.";
        Map<String, String> data = Map.of(
                "type", "sos",
                "patientName", patient);
        for (DeviceToken t : tokens) {
            fcmService.sendAlert(t.getToken(), title, body, data, "cuidapp_sos");
        }
        return ResponseEntity.ok(Map.of("sent", tokens.size()));
    }
}
