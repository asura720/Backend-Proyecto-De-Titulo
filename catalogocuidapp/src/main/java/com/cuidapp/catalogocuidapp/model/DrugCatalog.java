package com.cuidapp.catalogocuidapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drug_catalog")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DrugCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String barcode;

    private String name;
    private String genericName;
    private String brand;
    private String dosage;
    private String form;
    private String manufacturer;

    @Column(length = 2, columnDefinition = "VARCHAR(2) DEFAULT 'CL'")
    private String country;

    // "upcitemdb", "user", "isp_fnm", "manual"
    private String source;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean verified = false;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.country == null) this.country = "CL";
        if (this.verified == null) this.verified = false;
    }
}
