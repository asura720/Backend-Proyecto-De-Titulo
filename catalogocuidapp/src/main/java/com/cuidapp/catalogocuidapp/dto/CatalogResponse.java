package com.cuidapp.catalogocuidapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CatalogResponse {
    private Long id;
    private String barcode;
    private String name;
    private String genericName;
    private String brand;
    private String dosage;
    private String form;
    private String manufacturer;
    private String source;
    private Boolean verified;
}
