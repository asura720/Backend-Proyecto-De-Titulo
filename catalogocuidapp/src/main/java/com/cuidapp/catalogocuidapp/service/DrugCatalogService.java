package com.cuidapp.catalogocuidapp.service;

import com.cuidapp.catalogocuidapp.dto.CatalogResponse;
import com.cuidapp.catalogocuidapp.dto.OcrRequest;
import com.cuidapp.catalogocuidapp.dto.OcrResponse;
import com.cuidapp.catalogocuidapp.model.DrugCatalog;
import com.cuidapp.catalogocuidapp.repository.DrugCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
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

    @Value("${wikipedia.api.url:https://es.wikipedia.org/api/rest_v1/page/summary/}")
    private String wikipediaUrl;

    public Optional<CatalogResponse> findByBarcode(String barcode) {
        // 1. Buscar en catálogo local primero (y completar uso/efectos si faltan)
        Optional<DrugCatalog> local = repository.findByBarcode(barcode);
        if (local.isPresent()) {
            return Optional.of(toResponse(enrichInfo(local.get())));
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
                    // Completar uso/efectos desde internet con el nombre obtenido
                    return Optional.of(toResponse(enrichInfo(catalog)));
                }
            }
        } catch (Exception e) {
            // UPCitemdb no disponible o límite alcanzado — continuar sin fallar
            System.err.println("UPCitemdb lookup failed for " + barcode + ": " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Si al medicamento le falta la descripción de uso, la busca en Wikipedia
     * (por su principio activo o nombre) y la guarda. Devuelve el mismo objeto.
     */
    private DrugCatalog enrichInfo(DrugCatalog d) {
        if (d.getUso() != null && !d.getUso().isBlank()) return d;
        String q = (d.getGenericName() != null && !d.getGenericName().isBlank())
                ? d.getGenericName() : d.getName();
        if (q == null || q.isBlank()) return d;
        String extract = wikipediaExtract(q);
        if (extract != null) {
            d.setUso(extract);
            repository.save(d);
        }
        return d;
    }

    public List<CatalogResponse> search(String query) {
        List<DrugCatalog> local = repository.searchByName(query);
        if (!local.isEmpty()) {
            return local.stream().map(this::toResponse).toList();
        }
        // No está en el catálogo: buscar en internet (Wikipedia ES) y cachear.
        return fetchFromWikipedia(query)
                .map(d -> List.of(toResponse(d)))
                .orElseGet(List::of);
    }

    /**
     * Consulta el resumen de Wikipedia en español para el medicamento y, si lo
     * encuentra, lo guarda en el catálogo (caché) con su descripción de uso.
     */
    private Optional<DrugCatalog> fetchFromWikipedia(String query) {
        String title = query.trim().split("\\d")[0].trim();
        if (title.isEmpty()) title = query.trim();
        if (title.length() < 3) return Optional.empty();

        // Evitar volver a crear si ya existe uno guardado con ese nombre
        List<DrugCatalog> existing = repository.searchByName(title);
        if (!existing.isEmpty()) return Optional.of(existing.get(0));

        String extract = wikipediaExtract(title);
        if (extract == null) return Optional.empty();

        DrugCatalog c = DrugCatalog.builder()
                .name(title)
                .genericName(title)
                .uso(extract)
                .source("wikipedia")
                .country("CL")
                .verified(false)
                .build();
        return Optional.of(repository.save(c));
    }

    /** Trae el resumen de Wikipedia ES para un término. Devuelve null si no hay. */
    private String wikipediaExtract(String term) {
        try {
            String title = term.trim().split("\\d")[0].trim();
            if (title.length() < 3) return null;
            String url = wikipediaUrl
                    + UriUtils.encodePathSegment(title, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "CuidApp/1.0 (cuidappnoreply@gmail.com)");
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = resp.getBody();
            if (body == null) return null;
            String type = String.valueOf(body.get("type"));
            if (type.contains("not_found") || type.contains("disambiguation")) {
                return null;
            }
            String extract = (String) body.get("extract");
            return (extract == null || extract.isBlank()) ? null : extract;
        } catch (Exception e) {
            System.err.println("Wikipedia lookup failed for " + term + ": " + e.getMessage());
            return null;
        }
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
            .uso(d.getUso())
            .efectosSecundarios(d.getEfectosSecundarios())
            .source(d.getSource())
            .verified(d.getVerified())
            .build();
    }
}
