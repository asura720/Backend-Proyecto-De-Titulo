package com.cuidapp.medicamentoscuidapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_medications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String dosage;
    private String frequency;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> times;

    // Días de la semana en que se toma (1=Lunes ... 7=Domingo). Vacío = todos los días.
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> days;

    // Dosis ya tomadas, cada entrada "YYYY-MM-DD|HH:mm" (fecha + horario de la dosis).
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> takenDoses;

    private String containerColor;
    private String iconColor;     

    private Boolean isTaken = false;
    private LocalDateTime takenDateTime;

    private Long userId; // Relación con el usuario de la App

    // Compatibilidad con código existente que espera métodos isTaken() / setTaken(boolean)
    public boolean isTaken() {
        return Boolean.TRUE.equals(this.isTaken);
    }

    public void setTaken(boolean taken) {
        this.isTaken = taken;
    }
}
