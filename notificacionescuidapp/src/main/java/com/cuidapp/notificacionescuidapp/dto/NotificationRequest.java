package com.cuidapp.notificacionescuidapp.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class NotificationRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long medicationId;

    @NotNull
    private String medicationName;

    @NotNull
    private String scheduledAt; // ISO-8601 string

    private String title = "Recordatorio de medicamento";

    private String message;
}
