package com.cuidapp.autenticacioncuidapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vinculacion_cuidador")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VinculacionCuidador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id", nullable = false)
    private User titular;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private User paciente;

    // Si está activo, el paciente puede gestionar sus propios medicamentos y controles
    @Builder.Default
    @Column(nullable = false)
    private Boolean pacientePuedeGestionar = false;

    // El paciente solicitó permiso y el titular aún no responde
    @Builder.Default
    @Column(nullable = false)
    private Boolean solicitudPendiente = false;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
