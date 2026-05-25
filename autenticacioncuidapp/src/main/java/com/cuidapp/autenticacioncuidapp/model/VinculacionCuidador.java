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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVinculacion estado = EstadoVinculacion.PENDIENTE;

    // Token UUID enviado por email para aceptar/rechazar
    @Column(unique = true, nullable = false)
    private String token;

    private LocalDateTime tokenExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime respondidoAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.tokenExpiry == null) {
            this.tokenExpiry = LocalDateTime.now().plusHours(48);
        }
    }

    public boolean isTokenValido() {
        return LocalDateTime.now().isBefore(this.tokenExpiry);
    }

    public enum EstadoVinculacion {
        PENDIENTE, ACEPTADA, RECHAZADA
    }
}
