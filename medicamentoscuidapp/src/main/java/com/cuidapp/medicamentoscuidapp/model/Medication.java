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

    @ElementCollection
    private List<String> times;

    private String containerColor;
    private String iconColor;     

    private boolean isTaken = false;
    private LocalDateTime takenDateTime;

    private Long userId; // Relación con el usuario de la App
}
