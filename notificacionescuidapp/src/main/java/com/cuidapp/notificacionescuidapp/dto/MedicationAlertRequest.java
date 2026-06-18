package com.cuidapp.notificacionescuidapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MedicationAlertRequest {

    /** Usuario que debe recibir la alerta (cuidador o el mismo paciente). */
    @NotNull
    private Long targetUserId;

    /** Nombre del paciente (cuando se avisa al cuidador). Null si es para sí mismo. */
    private String patientName;

    /** Nombre del medicamento no tomado. */
    private String medicationName;
}
