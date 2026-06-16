package com.cuidapp.notificacionescuidapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WelcomeRequest {

    @NotNull
    private Long userId;

    private String name;
}
