package com.cuidapp.notificacionescuidapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SosRequest {

    /** Id del cuidador (titular) que debe recibir la alerta. */
    @NotNull
    private Long caregiverId;

    /** Nombre del paciente que pulsó el SOS (para el mensaje). */
    private String patientName;
}
