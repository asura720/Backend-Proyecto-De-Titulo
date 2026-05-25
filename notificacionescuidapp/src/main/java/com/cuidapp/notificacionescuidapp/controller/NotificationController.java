package com.cuidapp.notificacionescuidapp.controller;

import com.cuidapp.notificacionescuidapp.dto.NotificationRequest;
import com.cuidapp.notificacionescuidapp.model.Notification;
import com.cuidapp.notificacionescuidapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/schedule")
    public ResponseEntity<Notification> schedule(@Validated @RequestBody NotificationRequest req) {
        Notification saved = service.saveFromRequest(req);
        return ResponseEntity.ok(saved);
    }
}
