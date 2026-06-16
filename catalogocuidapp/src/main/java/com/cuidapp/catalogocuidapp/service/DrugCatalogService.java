package com.cuidapp.catalogocuidapp.service;

import com.cuidapp.catalogocuidapp.dto.CatalogResponse;
import com.cuidapp.catalogocuidapp.dto.OcrRequest;
import com.cuidapp.catalogocuidapp.dto.OcrResponse;
import com.cuidapp.catalogocuidapp.model.DrugCatalog;
import com.cuidapp.catalogocuidapp.repository.DrugCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DrugCatalogService {

    private final DrugCatalogRepository repository;
    private final OcrParserService ocrParser;
    private final RestTemplate restTemplate;

    @Value("${upcitemdb.api.url}")
    private String upcItemDbUrl;

    public Optional<CatalogResponse> findByBarcode(String barcode) {
        // 1. Buscar en catálogo local primero
        Optional<DrugCatalog> local = repository.findByBarcode(barcode);
        if (local.isPresent()) {
            return Optional.of(toResponse(local.get()));
        }

        // 2. Fallback a UPCitemdb (100 req/día gratis)
        try {
            String url = upcItemDbUrl + "?upc=" + barcode;
            Map<?, ?> result = restTemplate.getForObject(url, Map.class);
            if (result != null && "OK".equals(result.get("code"))) {
                List<?> items = (List<?>) result.get("items");
                if (items != null && !items.isEmpty()) {
                    Map<?, ?> item = (Map<?, ?>) items.get(0);
                    DrugCatalog catalog = buildFromUpcItemDb(barcode, item);
                    repository.save(catalog);
                    return Optional.of(toResponse(catalog));
                }
            }
        } catch (Exception e) {
            // UPCitemdb no disponible o límite alcanzado — continuar sin fallar
            System.err.println("UPCitemdb lookup failed for " + barcode + ": " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<CatalogResponse> search(String query) {
        return repository.searchByName(query).stream().map(this::toResponse).toList();
    }

    public List<CatalogResponse> suggest(String prefix) {
        return repository.findByNameStartingWith(prefix).stream().limit(10).map(this::toResponse).toList();
    }

    public CatalogResponse save(DrugCatalog drug) {
        if (drug.getSource() == null) drug.setSource("user");
        return toResponse(repository.save(drug));
    }

    public OcrResponse parseOcr(OcrRequest request) {
        return ocrParser.parse(request.getText());
    }

    private DrugCatalog buildFromUpcItemDb(String barcode, Map<?, ?> item) {
        String title = (String) item.get("title");
        String brand = (String) item.get("brand");

        return DrugCatalog.builder()
            .barcode(barcode)
            .name(title)
            .brand(brand)
            .source("upcitemdb")
            .verified(false)
            .build();
    }

    private CatalogResponse toResponse(DrugCatalog d) {
        return CatalogResponse.builder()
            .id(d.getId())
            .barcode(d.getBarcode())
            .name(d.getName())
            .genericName(d.getGenericName())
            .brand(d.getBrand())
            .dosage(d.getDosage())
            .form(d.getForm())
            .manufacturer(d.getManufacturer())
            .source(d.getSource())
            .verified(d.getVerified())
            .build();
    }
}
