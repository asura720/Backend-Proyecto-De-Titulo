package com.cuidapp.autenticacioncuidapp.model;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;

    @JsonProperty(access = Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    private String name;
    private String phone;

    // Cambiado para coincidir con birthDate en Flutter
    @Column(name = "birth_date")
    private LocalDate birthDate;

    // Cambiado para coincidir con emergencyContact en Flutter
    @Column(name = "emergency_contact")
    private String emergencyContact;

    // Cambiado para coincidir con emergencyPhone en Flutter
    @Column(name = "emergency_phone")
    private String emergencyPhone; 

    // Cambiado para coincidir con bloodType en Flutter
    @Column(name = "blood_type")
    private String bloodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.INDEPENDIENTE;
}